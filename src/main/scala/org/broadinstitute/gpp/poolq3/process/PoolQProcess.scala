/*
 * Copyright (c) 2024 The Broad Institute, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */
package org.broadinstitute.gpp.poolq3.process

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

  private val queue: ArrayBlockingQueue[Barcodes] = new ArrayBlockingQueue(queueSize)

  @volatile private var done = false

  final private class ConsumerThread extends Thread:

    override def run(): Unit =
      val t0 = System.currentTimeMillis()

      def logProgress(n: Int): Unit =
        val nd = consumer.readsProcessed.toFloat
        val dt = System.currentTimeMillis() - t0
        val avg = nd / dt
        val pct = consumer.matchPercent
        log.info(s"Processed $n reads in $dt ms ($avg reads/ms). Match percent: $pct; queue size: ${queue.size()}")

      while !done || !queue.isEmpty do // as long as we're not done OR there is still work in the queue
        try Option(queue.poll(100, TimeUnit.MILLISECONDS)).foreach(next => consumer.consume(next))
        catch
          case _: InterruptedException =>
            log.warn(
              s"Interrupted. Done = $done Processed ${consumer.readsProcessed} reads; queue has ${queue.size()} remaining"
            )
          case NonFatal(e) => log.error(e)(s"Error processing read ${consumer.readsProcessed}")
        // update the log periodically
        val n = consumer.readsProcessed
        if n % reportFrequency == 0 then logProgress(n)
      end while
      logProgress(consumer.readsProcessed)

    end run

  end ConsumerThread

  /** Runs the process in the calling thread and returns the final state */
  def run(): PoolQRunSummary =
    val consumerThread = new ConsumerThread
    consumerThread.setName("Consumer")

    log.info("Beginning task processing.")
    consumer.start()
    consumerThread.start()

    // fill the queue
    source.foreach(queue.put)

    // signal the end
    done = true

    // shut down the processing thread
    log.info("Shutting down.")
    consumerThread.join()

    consumer.close()

    PoolQRunSummary(consumer.readsProcessed, consumer.matchingReads, consumer.matchPercent, consumer.state)

  end run

end PoolQProcess
