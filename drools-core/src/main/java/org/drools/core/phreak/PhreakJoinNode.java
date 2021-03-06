package org.drools.core.phreak;

import org.drools.core.common.BetaConstraints;
import org.drools.core.common.InternalWorkingMemory;
import org.drools.core.common.LeftTupleSets;
import org.drools.core.common.RightTupleSets;
import org.drools.core.reteoo.BetaMemory;
import org.drools.core.reteoo.JoinNode;
import org.drools.core.reteoo.LeftTuple;
import org.drools.core.reteoo.LeftTupleMemory;
import org.drools.core.reteoo.LeftTupleSink;
import org.drools.core.reteoo.RightTuple;
import org.drools.core.reteoo.RightTupleMemory;
import org.drools.core.rule.ContextEntry;
import org.drools.core.spi.PropagationContext;
import org.drools.core.util.FastIterator;

/**
* Created with IntelliJ IDEA.
* User: mdproctor
* Date: 03/05/2013
* Time: 15:41
* To change this template use File | Settings | File Templates.
*/
public class PhreakJoinNode {
    public void doNode(JoinNode joinNode,
                       LeftTupleSink sink,
                       BetaMemory bm,
                       InternalWorkingMemory wm,
                       LeftTupleSets srcLeftTuples,
                       LeftTupleSets trgLeftTuples,
                       LeftTupleSets stagedLeftTuples) {

        RightTupleSets srcRightTuples = bm.getStagedRightTuples();

        if (srcRightTuples.getDeleteFirst() != null) {
            doRightDeletes(joinNode, bm, wm, srcRightTuples, trgLeftTuples, stagedLeftTuples);
        }

        if (srcLeftTuples.getDeleteFirst() != null) {
            doLeftDeletes(joinNode, bm, wm, srcLeftTuples, trgLeftTuples, stagedLeftTuples);
        }

        if (srcLeftTuples.getUpdateFirst() != null ) {
            RuleNetworkEvaluator.dpUpdatesReorderLeftMemory(bm,
                                                            srcLeftTuples);
        }

        if (srcRightTuples.getUpdateFirst() != null) {
            RuleNetworkEvaluator.dpUpdatesReorderRightMemory(bm,
                                                             srcRightTuples);
        }

        if (srcRightTuples.getUpdateFirst() != null) {
            doRightUpdates(joinNode, sink, bm, wm, srcRightTuples, trgLeftTuples, stagedLeftTuples);
        }

        if (srcLeftTuples.getUpdateFirst() != null) {
            doLeftUpdates(joinNode, sink, bm, wm, srcLeftTuples, trgLeftTuples, stagedLeftTuples);
        }

        if (srcRightTuples.getInsertFirst() != null) {
            doRightInserts(joinNode, sink, bm, wm, srcRightTuples, trgLeftTuples);
        }

        if (srcLeftTuples.getInsertFirst() != null) {
            doLeftInserts(joinNode, sink, bm, wm, srcLeftTuples, trgLeftTuples);
        }

        srcRightTuples.resetAll();
        srcLeftTuples.resetAll();
    }

    public void doLeftInserts(JoinNode joinNode,
                              LeftTupleSink sink,
                              BetaMemory bm,
                              InternalWorkingMemory wm,
                              LeftTupleSets srcLeftTuples,
                              LeftTupleSets trgLeftTuples) {
        LeftTupleMemory ltm = bm.getLeftTupleMemory();
        RightTupleMemory rtm = bm.getRightTupleMemory();
        ContextEntry[] contextEntry = bm.getContext();
        BetaConstraints constraints = joinNode.getRawConstraints();

        for (LeftTuple leftTuple = srcLeftTuples.getInsertFirst(); leftTuple != null; ) {
            LeftTuple next = leftTuple.getStagedNext();

            boolean useLeftMemory = RuleNetworkEvaluator.useLeftMemory(joinNode, leftTuple);

            if (useLeftMemory) {
                ltm.add(leftTuple);
            }

            FastIterator it = joinNode.getRightIterator(rtm);
            PropagationContext context = leftTuple.getPropagationContext();

            constraints.updateFromTuple(contextEntry,
                                        wm,
                                        leftTuple);

            for (RightTuple rightTuple = joinNode.getFirstRightTuple(leftTuple,
                                                                     rtm,
                                                                     null,
                                                                     it); rightTuple != null; rightTuple = (RightTuple) it.next(rightTuple)) {
                if (constraints.isAllowedCachedLeft(contextEntry,
                                                    rightTuple.getFactHandle())) {
                    trgLeftTuples.addInsert(sink.createLeftTuple(leftTuple,
                                                                 rightTuple,
                                                                 null,
                                                                 null,
                                                                 sink,
                                                                 useLeftMemory));
                }

            }
            leftTuple.clearStaged();
            leftTuple = next;
        }
        constraints.resetTuple(contextEntry);
    }

    public void doRightInserts(JoinNode joinNode,
                               LeftTupleSink sink,
                               BetaMemory bm,
                               InternalWorkingMemory wm,
                               RightTupleSets srcRightTuples,
                               LeftTupleSets trgLeftTuples) {
        LeftTupleMemory ltm = bm.getLeftTupleMemory();
        RightTupleMemory rtm = bm.getRightTupleMemory();
        ContextEntry[] contextEntry = bm.getContext();
        BetaConstraints constraints = joinNode.getRawConstraints();

        for (RightTuple rightTuple = srcRightTuples.getInsertFirst(); rightTuple != null; ) {
            RightTuple next = rightTuple.getStagedNext();

            rtm.add(rightTuple);

            FastIterator it = joinNode.getLeftIterator(ltm);
            PropagationContext context = rightTuple.getPropagationContext();

            constraints.updateFromFactHandle(contextEntry,
                                             wm,
                                             rightTuple.getFactHandle());

            for (LeftTuple leftTuple = joinNode.getFirstLeftTuple(rightTuple, ltm, context, it); leftTuple != null; leftTuple = (LeftTuple) it.next(leftTuple)) {
                if (leftTuple.getStagedType() == LeftTuple.UPDATE) {
                    // ignore, as it will get processed via left iteration. Children cannot be processed twice
                    continue;
                }

                if (constraints.isAllowedCachedRight(contextEntry,
                                                     leftTuple)) {
                    trgLeftTuples.addInsert(sink.createLeftTuple(leftTuple,
                                                                 rightTuple,
                                                                 null,
                                                                 null,
                                                                 sink,
                                                                 true));
                }
            }
            rightTuple.clearStaged();
            rightTuple = next;
        }
        constraints.resetFactHandle(contextEntry);
    }

    public void doLeftUpdates(JoinNode joinNode,
                              LeftTupleSink sink,
                              BetaMemory bm,
                              InternalWorkingMemory wm,
                              LeftTupleSets srcLeftTuples,
                              LeftTupleSets trgLeftTuples,
                              LeftTupleSets stagedLeftTuples) {
        RightTupleMemory rtm = bm.getRightTupleMemory();
        ContextEntry[] contextEntry = bm.getContext();
        BetaConstraints constraints = joinNode.getRawConstraints();

        for (LeftTuple leftTuple = srcLeftTuples.getUpdateFirst(); leftTuple != null; ) {
            LeftTuple next = leftTuple.getStagedNext();

            PropagationContext context = leftTuple.getPropagationContext();

            constraints.updateFromTuple(contextEntry,
                                        wm,
                                        leftTuple);

            FastIterator it = joinNode.getRightIterator(rtm);
            RightTuple rightTuple = joinNode.getFirstRightTuple(leftTuple,
                                                                rtm,
                                                                null,
                                                                it);

            LeftTuple childLeftTuple = leftTuple.getFirstChild();

            // first check our index (for indexed nodes only) hasn't changed and we are returning the same bucket
            // if rightTuple is null, we assume there was a bucket change and that bucket is empty
            if (childLeftTuple != null && rtm.isIndexed() && !it.isFullIterator() && (rightTuple == null || (rightTuple.getMemory() != childLeftTuple.getRightParent().getMemory()))) {
                // our index has changed, so delete all the previous propagations
                while (childLeftTuple != null) {
                    childLeftTuple = RuleNetworkEvaluator.deleteLeftChild(childLeftTuple, trgLeftTuples, stagedLeftTuples);
                }
                // childLeftTuple is now null, so the next check will attempt matches for new bucket
            }

            // we can't do anything if RightTupleMemory is empty
            if (rightTuple != null) {
                doLeftUpdatesProcessChildren(childLeftTuple, leftTuple, rightTuple, stagedLeftTuples, contextEntry, constraints, sink, it, trgLeftTuples);
            }
            leftTuple.clearStaged();
            leftTuple = next;
        }
        constraints.resetTuple(contextEntry);
    }

    public LeftTuple doLeftUpdatesProcessChildren(LeftTuple childLeftTuple,
                                                  LeftTuple leftTuple,
                                                  RightTuple rightTuple,
                                                  LeftTupleSets stagedLeftTuples,
                                                  ContextEntry[] contextEntry,
                                                  BetaConstraints constraints,
                                                  LeftTupleSink sink,
                                                  FastIterator it,
                                                  LeftTupleSets trgLeftTuples) {
        if (childLeftTuple == null) {
            // either we are indexed and changed buckets or
            // we had no children before, but there is a bucket to potentially match, so try as normal assert
            for (; rightTuple != null; rightTuple = (RightTuple) it.next(rightTuple)) {
                if (constraints.isAllowedCachedLeft(contextEntry,
                                                    rightTuple.getFactHandle())) {
                    trgLeftTuples.addInsert(sink.createLeftTuple(leftTuple,
                                                                 rightTuple,
                                                                 null,
                                                                 null,
                                                                 sink,
                                                                 true));
                }
            }
        } else {
            // in the same bucket, so iterate and compare
            for (; rightTuple != null; rightTuple = (RightTuple) it.next(rightTuple)) {
                if (constraints.isAllowedCachedLeft(contextEntry,
                                                    rightTuple.getFactHandle())) {
                    // insert, childLeftTuple is not updated
                    if (childLeftTuple == null || childLeftTuple.getRightParent() != rightTuple) {
                        trgLeftTuples.addInsert(sink.createLeftTuple(leftTuple,
                                                                     rightTuple,
                                                                     childLeftTuple,
                                                                     null,
                                                                     sink,
                                                                     true));
                    } else {
                        switch (childLeftTuple.getStagedType()) {
                            // handle clash with already staged entries
                            case LeftTuple.INSERT:
                                stagedLeftTuples.removeInsert(childLeftTuple);
                                break;
                            case LeftTuple.UPDATE:
                                stagedLeftTuples.removeUpdate(childLeftTuple);
                                break;
                        }

                        // update, childLeftTuple is updated
                        trgLeftTuples.addUpdate(childLeftTuple);

                        LeftTuple nextChildLeftTuple = childLeftTuple.getLeftParentNext();
                        childLeftTuple.reAddRight();
                        childLeftTuple = nextChildLeftTuple;
                    }
                } else if (childLeftTuple != null && childLeftTuple.getRightParent() == rightTuple) {
                    // delete, childLeftTuple is updated
                    childLeftTuple = RuleNetworkEvaluator.deleteLeftChild(childLeftTuple, trgLeftTuples, stagedLeftTuples);
                }
            }
        }

        return childLeftTuple;
    }

    public void doRightUpdates(JoinNode joinNode,
                               LeftTupleSink sink,
                               BetaMemory bm,
                               InternalWorkingMemory wm,
                               RightTupleSets srcRightTuples,
                               LeftTupleSets trgLeftTuples,
                               LeftTupleSets stagedLeftTuples) {
        LeftTupleMemory ltm = bm.getLeftTupleMemory();
        ContextEntry[] contextEntry = bm.getContext();
        BetaConstraints constraints = joinNode.getRawConstraints();

        for (RightTuple rightTuple = srcRightTuples.getUpdateFirst(); rightTuple != null; ) {
            RightTuple next = rightTuple.getStagedNext();

            PropagationContext context = rightTuple.getPropagationContext();

            LeftTuple childLeftTuple = rightTuple.getFirstChild();

            FastIterator it = joinNode.getLeftIterator(ltm);
            LeftTuple leftTuple = joinNode.getFirstLeftTuple(rightTuple, ltm, context, it);

            constraints.updateFromFactHandle(contextEntry,
                                             wm,
                                             rightTuple.getFactHandle());

            // first check our index (for indexed nodes only) hasn't changed and we are returning the same bucket
            // We assume a bucket change if leftTuple == null
            if (childLeftTuple != null && ltm.isIndexed() && !it.isFullIterator() && (leftTuple == null || (leftTuple.getMemory() != childLeftTuple.getLeftParent().getMemory()))) {
                // our index has changed, so delete all the previous propagations
                while (childLeftTuple != null) {
                    childLeftTuple = RuleNetworkEvaluator.deleteRightChild(childLeftTuple, trgLeftTuples, stagedLeftTuples);
                }
                // childLeftTuple is now null, so the next check will attempt matches for new bucket
            }

            // we can't do anything if LeftTupleMemory is empty
            if (leftTuple != null) {
                doRightUpdatesProcessChildren(childLeftTuple, leftTuple, rightTuple, stagedLeftTuples, contextEntry, constraints, sink, it, trgLeftTuples);
            }
            rightTuple.clearStaged();
            rightTuple = next;
        }
        constraints.resetFactHandle(contextEntry);
    }

    public LeftTuple doRightUpdatesProcessChildren(LeftTuple childLeftTuple,
                                                   LeftTuple leftTuple,
                                                   RightTuple rightTuple,
                                                   LeftTupleSets stagedLeftTuples,
                                                   ContextEntry[] contextEntry,
                                                   BetaConstraints constraints,
                                                   LeftTupleSink sink,
                                                   FastIterator it,
                                                   LeftTupleSets trgLeftTuples) {
        if (childLeftTuple == null) {
            // either we are indexed and changed buckets or
            // we had no children before, but there is a bucket to potentially match, so try as normal assert
            for (; leftTuple != null; leftTuple = (LeftTuple) it.next(leftTuple)) {
                if (leftTuple.getStagedType() == LeftTuple.UPDATE) {
                    // ignore, as it will get processed via left iteration. Children cannot be processed twice
                    continue;
                }

                if (constraints.isAllowedCachedRight(contextEntry,
                                                     leftTuple)) {
                    trgLeftTuples.addInsert(sink.createLeftTuple(leftTuple,
                                                                 rightTuple,
                                                                 null,
                                                                 null,
                                                                 sink,
                                                                 true));
                }
            }
        } else {
            // in the same bucket, so iterate and compare
            for (; leftTuple != null; leftTuple = (LeftTuple) it.next(leftTuple)) {
                if (leftTuple.getStagedType() == LeftTuple.UPDATE) {
                    // ignore, as it will get processed via left iteration. Children cannot be processed twice
                    continue;
                }

                if (constraints.isAllowedCachedRight(contextEntry,
                                                     leftTuple)) {
                    // insert, childLeftTuple is not updated
                    if (childLeftTuple == null || childLeftTuple.getLeftParent() != leftTuple) {
                        trgLeftTuples.addInsert(sink.createLeftTuple(leftTuple,
                                                                     rightTuple,
                                                                     null,
                                                                     childLeftTuple,
                                                                     sink,
                                                                     true));
                    } else {
                        switch (childLeftTuple.getStagedType()) {
                            // handle clash with already staged entries
                            case LeftTuple.INSERT:
                                stagedLeftTuples.removeInsert(childLeftTuple);
                                break;
                            case LeftTuple.UPDATE:
                                stagedLeftTuples.removeUpdate(childLeftTuple);
                                break;
                        }

                        // update, childLeftTuple is updated
                        trgLeftTuples.addUpdate(childLeftTuple);

                        LeftTuple nextChildLeftTuple = childLeftTuple.getRightParentNext();
                        childLeftTuple.reAddLeft();
                        childLeftTuple = nextChildLeftTuple;
                    }
                } else if (childLeftTuple != null && childLeftTuple.getLeftParent() == leftTuple) {
                    // delete, childLeftTuple is updated
                    childLeftTuple = RuleNetworkEvaluator.deleteRightChild(childLeftTuple, trgLeftTuples, stagedLeftTuples);
                }
            }
        }

        return childLeftTuple;
    }

    public void doLeftDeletes(JoinNode joinNode,
                              BetaMemory bm,
                              InternalWorkingMemory wm,
                              LeftTupleSets srcLeftTuples,
                              LeftTupleSets trgLeftTuples,
                              LeftTupleSets stagedLeftTuples) {
        LeftTupleMemory ltm = bm.getLeftTupleMemory();

        for (LeftTuple leftTuple = srcLeftTuples.getDeleteFirst(); leftTuple != null; ) {
            LeftTuple next = leftTuple.getStagedNext();
            if (leftTuple.getMemory() != null) {
                // it may have been staged and never actually added
                ltm.remove(leftTuple);
            }

            if (leftTuple.getFirstChild() != null) {
                LeftTuple childLeftTuple = leftTuple.getFirstChild();

                while (childLeftTuple != null) {
                    childLeftTuple = RuleNetworkEvaluator.deleteLeftChild(childLeftTuple, trgLeftTuples, stagedLeftTuples);
                }
            }
            leftTuple.clearStaged();
            leftTuple = next;
        }
    }

    public void doRightDeletes(JoinNode joinNode,
                               BetaMemory bm,
                               InternalWorkingMemory wm,
                               RightTupleSets srcRightTuples,
                               LeftTupleSets trgLeftTuples,
                               LeftTupleSets stagedLeftTuples) {
        RightTupleMemory rtm = bm.getRightTupleMemory();

        for (RightTuple rightTuple = srcRightTuples.getDeleteFirst(); rightTuple != null; ) {
            RightTuple next = rightTuple.getStagedNext();
            if (rightTuple.getMemory() != null) {
                // it may have been staged and never actually added
                rtm.remove(rightTuple);
            }
            ;

            if (rightTuple.getFirstChild() != null) {
                LeftTuple childLeftTuple = rightTuple.getFirstChild();

                while (childLeftTuple != null) {
                    childLeftTuple = RuleNetworkEvaluator.deleteRightChild(childLeftTuple, trgLeftTuples, stagedLeftTuples);
                }
            }
            rightTuple.clearStaged();
            rightTuple = next;
        }
    }
}
