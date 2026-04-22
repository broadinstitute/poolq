/*
 * Copyright (c) 2024 The Broad Institute, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */
package org.broadinstitute.gpp.poolq3.process

import java.text.NumberFormat
import java.util.Locale
import java.util.concurrent.{ArrayBlockingQueue, TimeUnit}

import scala.util.control.NonFatal

import org.broadinstitute.gpp.poolq3.barcode.Barcodes
import org.broadinstitute.gpp.poolq3.types.PoolQRunSummary
import org.log4s.{Logger, getLogger}

/** A simple, decoupled single-source, single consumer process implementation. The source is an iterator of
  * [[org.broadinstitute.gpp.poolq3.barcode.Barcodes]]. The consumer consumes
  * [[org.broadinstitute.gpp.poolq3.barcode.Barcodes]], aggregating them into a [[State]]. The consumer reports progress
  * periodically.
  */
final class PoolQProcess(
    source: Iterator[Barcodes],
    consumer: Consumer,
    queueSize: Int = 100,
    reportFrequency: Int = 5000000
):

  private val log: Logger = getLogger

  private val nf: NumberFormat = NumberFormat.getInstance(Locale.getDefault())

  private val queue: ArrayBlockingQueue[Barcodes] = new ArrayBlockingQueue(queueSize)

  private val queueWaitMillis: Long = 100L

  @volatile private var done = false

  @volatile private var consumerFailure: Throwable | Null = null

  final private class ConsumerThread extends Thread:

    override def run(): Unit =
      val t0 = System.currentTimeMillis()

      def logProgress(n: Long): Unit =
        val nd = consumer.readsProcessed.toFloat
        val dt = System.currentTimeMillis() - t0
        val avg = nd / dt
        val pct = consumer.matchPercent
        log.info(
          s"Processed ${nf.format(n)} reads in $dt ms ($avg reads/ms). Match percent: $pct; queue size: ${queue.size()}"
        )
      end logProgress

      while consumerFailure == null && (!done || !queue.isEmpty) do
        try Option(queue.poll(100, TimeUnit.MILLISECONDS)).foreach(next => consumer.consume(next))
        catch
          case _: InterruptedException =>
            log.warn(
              s"Interrupted. Done = $done Processed ${nf.format(consumer.readsProcessed)} reads; queue has ${queue.size()} remaining"
            )
          case NonFatal(e) =>
            consumerFailure = e
            done = true
            log.error(e)(s"Error processing read ${consumer.readsProcessed}; terminating run")
        end try
        // update the log periodically
        val n = consumer.readsProcessed
        if n % reportFrequency == 0L then logProgress(n)
      end while
      logProgress(consumer.readsProcessed)

    end run

  end ConsumerThread

  /** Runs the process in the calling thread and returns the final state */
  def run(): PoolQRunSummary =
    val consumerThread = new ConsumerThread
    consumerThread.setName("Consumer")
    var consumerClosed = false

    def closeConsumer(primaryErrorOpt: Option[Throwable]): Unit =
      if !consumerClosed then
        try
          consumer.close()
          consumerClosed = true
        catch
          case NonFatal(closeErr) =>
            primaryErrorOpt match
              case Some(primaryError) => primaryError.addSuppressed(closeErr)
              case None => throw closeErr
      end if
    end closeConsumer

    // Keep retrying until an item is successfully enqueued, but fail fast if the consumer has errored.
    def enqueueOrFail(next: Barcodes): Unit =
      var enqueued = false
      while !enqueued do
        val failure = consumerFailure
        if failure != null then throw failure
        enqueued = queue.offer(next, queueWaitMillis, TimeUnit.MILLISECONDS)

    log.info("Beginning task processing.")
    consumer.start()
    consumerThread.start()

    try
      // fill the queue while checking for failures in the consumer thread
      while source.hasNext && consumerFailure == null do
        val next = source.next()
        enqueueOrFail(next)

      // signal the end
      done = true

      // shut down the processing thread
      log.info("Shutting down.")
      consumerThread.join()

      val failure = Option(consumerFailure)
      closeConsumer(failure)
      failure.foreach(throw _)

      PoolQRunSummary(consumer.readsProcessed, consumer.matchingReads, consumer.matchPercent, consumer.state)
    catch
      case t: Throwable =>
        done = true
        consumerThread.interrupt()
        consumerThread.join()
        closeConsumer(Some(t))
        throw t
    end try

  end run

end PoolQProcess
