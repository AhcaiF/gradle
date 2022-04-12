/*
 * Copyright 2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.internal.execution.steps;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSortedMap;
import org.gradle.api.file.FileCollection;
import org.gradle.internal.Try;
import org.gradle.internal.execution.ExecutionResult;
import org.gradle.internal.execution.OutputChangeListener;
import org.gradle.internal.execution.UnitOfWork;
import org.gradle.internal.execution.WorkValidationContext;
import org.gradle.internal.execution.history.BeforeExecutionState;
import org.gradle.internal.execution.history.ExecutionHistoryStore;
import org.gradle.internal.execution.history.PreviousExecutionState;
import org.gradle.internal.execution.history.changes.InputChangesInternal;
import org.gradle.internal.file.TreeType;
import org.gradle.internal.fingerprint.CurrentFileCollectionFingerprint;
import org.gradle.internal.snapshot.ValueSnapshot;

import java.io.File;
import java.time.Duration;
import java.util.Optional;

public class BroadcastChangingOutputsStep<C extends InputChangesContext, R extends Result> implements Step<C, ChangesToOutputsFinishedResult> {

    private final OutputChangeListener outputChangeListener;
    private final Step<? super ChangesOutputContext, ? extends R> delegate;

    public BroadcastChangingOutputsStep(
        OutputChangeListener outputChangeListener,
        Step<? super ChangesOutputContext, ? extends R> delegate
    ) {
        this.outputChangeListener = outputChangeListener;
        this.delegate = delegate;
    }

    @Override
    public ChangesToOutputsFinishedResult execute(UnitOfWork work, C context) {
        ImmutableList.Builder<String> builder = ImmutableList.builder();
        work.visitOutputs(context.getWorkspace(), new UnitOfWork.OutputVisitor() {
            @Override
            public void visitOutputProperty(String propertyName, TreeType type, File root, FileCollection contents) {
                builder.add(root.getAbsolutePath());
            }

            @Override
            public void visitLocalState(File localStateRoot) {
                builder.add(localStateRoot.getAbsolutePath());
            }

            @Override
            public void visitDestroyable(File destroyableRoot) {
                builder.add(destroyableRoot.getAbsolutePath());
            }
        });
        ImmutableList<String> outputs = builder.build();
        outputChangeListener.beforeOutputChange(outputs);
        try {
            return wrapInChangesToOutputsFinishedResult(
                delegate.execute(work, wrapInChangesOutputsContext(context))
            );
        } finally {
            outputChangeListener.beforeOutputChange(outputs);
        }
    }

    private ChangesToOutputsFinishedResult wrapInChangesToOutputsFinishedResult(R result) {
        return new ChangesToOutputsFinishedResult() {
            @Override
            public Duration getDuration() {
                return result.getDuration();
            }

            @Override
            public Try<ExecutionResult> getExecutionResult() {
                return result.getExecutionResult();
            }
        };
    }

    private ChangesOutputContext wrapInChangesOutputsContext(C context) {
        return new ChangesOutputContext() {

            @Override
            public File getWorkspace() {
                return context.getWorkspace();
            }

            @Override
            public Optional<ExecutionHistoryStore> getHistory() {
                return context.getHistory();
            }

            @Override
            public Optional<ValidationResult> getValidationProblems() {
                return context.getValidationProblems();
            }

            @Override
            public Optional<PreviousExecutionState> getPreviousExecutionState() {
                return context.getPreviousExecutionState();
            }

            @Override
            public Optional<InputChangesInternal> getInputChanges() {
                return context.getInputChanges();
            }

            @Override
            public boolean isIncrementalExecution() {
                return context.isIncrementalExecution();
            }

            @Override
            public ImmutableSortedMap<String, ValueSnapshot> getInputProperties() {
                return context.getInputProperties();
            }

            @Override
            public ImmutableSortedMap<String, CurrentFileCollectionFingerprint> getInputFileProperties() {
                return context.getInputFileProperties();
            }

            @Override
            public UnitOfWork.Identity getIdentity() {
                return context.getIdentity();
            }

            @Override
            public Optional<String> getNonIncrementalReason() {
                return context.getNonIncrementalReason();
            }

            @Override
            public WorkValidationContext getValidationContext() {
                return context.getValidationContext();
            }

            @Override
            public Optional<BeforeExecutionState> getBeforeExecutionState() {
                return context.getBeforeExecutionState();
            }
        };
    }
}
