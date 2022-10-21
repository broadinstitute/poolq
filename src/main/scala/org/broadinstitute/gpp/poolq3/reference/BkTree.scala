/*
 * Copyright (c) 2022 The Broad Institute, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */
package org.broadinstitute.gpp.poolq3.reference

import org.broadinstitute.gpp.poolq3.reference.BkTree._

final class BkTree[A](dist: (A, A) => Int, dictionary: Seq[A]) {
  require(dictionary.nonEmpty, "Dictionary must be non-empty")

  private[this] def empty: Array[Node[A]] = Array.fill(8)(E)

  private[this] def extend(a: Array[Node[A]], minExtension: Int): Array[Node[A]] =
    Array.concat(a, Array.fill(math.max(a.length, minExtension - a.length + 1))(E))

  private[this] val root: Node[A] = {
    val initial: Node[A] = N(dictionary.head, empty)

    def insert(t: Node[A], s: A): Node[A] =
      t match {
        case E => N(s, empty)
        case n @ N(v, c) =>
          val d = dist(v, s)
          if (c.length <= d) {
            val nc = extend(c, d)
            nc(d) = N(s, empty)
            N(n.value, nc)
          } else {
            c(d) = insert(c(d), s)
            n
          }
      }

    dictionary.drop(1).foldLeft(initial)(insert)
  }

  def query(s: A, n: Int): Set[A] = {
    var children: List[A] = Nil
    def loop(node: Node[A]): Unit =
      node match {
        case E => ()
        case N(v, c) =>
          val d = dist(v, s)
          val range = math.max(0, d - n) to math.min(d + n, c.length - 1)
          range.foreach(x => loop(c(x)))
          if (d <= n) children ::= v
          ()
      }

    loop(root)
    children.toSet
  }

}

object BkTree {

  sealed private[BkTree] trait Node[+T]
  private[BkTree] case object E extends Node[Nothing]
  final private[BkTree] case class N[T](value: T, children: Array[Node[T]]) extends Node[T]

}
