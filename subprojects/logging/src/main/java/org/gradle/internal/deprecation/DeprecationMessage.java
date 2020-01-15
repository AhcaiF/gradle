/*
 * Copyright 2020 the original author or authors.
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

package org.gradle.internal.deprecation;

import com.google.common.base.Joiner;
import org.gradle.api.internal.DocumentationRegistry;
import org.gradle.internal.featurelifecycle.DeprecatedFeatureUsage;

import java.util.List;

import static org.gradle.internal.deprecation.Messages.pleaseUseThisMethodInstead;
import static org.gradle.internal.deprecation.Messages.thisBehaviourHasBeenDeprecatedAndIsScheduledToBeRemoved;
import static org.gradle.internal.deprecation.Messages.thisHasBeenDeprecatedAndIsScheduledToBeRemoved;
import static org.gradle.internal.deprecation.Messages.thisIsScheduledToBeRemoved;
import static org.gradle.internal.deprecation.Messages.thisWillBecomeAnError;
import static org.gradle.internal.deprecation.Messages.xHasBeenDeprecated;

public class DeprecationMessage {

    private static final DocumentationRegistry DOCUMENTATION_REGISTRY = new DocumentationRegistry();

    private final String summary;
    private final String removalDetails;
    private final String advice;
    private final String contextualAdvice;
    private final String documentationReference;
    private final DeprecatedFeatureUsage.Type usageType;

    public DeprecationMessage(String summary, String removalDetails, String advice, String contextualAdvice, String documentationReference, DeprecatedFeatureUsage.Type usageType) {
        this.summary = summary;
        this.removalDetails = removalDetails;
        this.advice = advice;
        this.contextualAdvice = contextualAdvice;
        this.documentationReference = documentationReference;
        this.usageType = usageType;
    }

    public static class Builder {
        private String summary;
        private String removalDetails;
        private String advice;
        private String contextualAdvice;
        private String documentationReference;

        private DeprecatedFeatureUsage.Type usageType = DeprecatedFeatureUsage.Type.USER_CODE_DIRECT;

        public Builder withSummary(String summary) {
            this.summary = summary;
            return this;
        }

        public Builder withRemovalDetails(String removalDetails) {
            this.removalDetails = removalDetails;
            return this;
        }

        public Builder withAdvice(String advice) {
            this.advice = advice;
            return this;
        }

        public Builder withContextualAdvice(String contextualAdvice) {
            this.contextualAdvice = contextualAdvice;
            return this;
        }

        public Builder withDocumentationReference(String documentationReference) {
            this.documentationReference = documentationReference;
            return this;
        }

        public Builder withIndirectUsage() {
            this.usageType = DeprecatedFeatureUsage.Type.USER_CODE_INDIRECT;
            return this;
        }

        public Builder withBuildInvocation() {
            this.usageType = DeprecatedFeatureUsage.Type.BUILD_INVOCATION;
            return this;
        }

        public void nagUser() {
            DeprecationLogger.nagUserWith(this, DeprecationMessage.Builder.class);
        }

        DeprecationMessage build() {
            return new DeprecationMessage(summary, removalDetails, advice, contextualAdvice, documentationReference, usageType);
        }

    }

    public static abstract class DeprecationWithReplacementBuilder<T> extends Builder {
        private final String subject;
        private T replacement;

        DeprecationWithReplacementBuilder(String subject) {
            this.subject = subject;
        }

        public DeprecationWithReplacementBuilder<T> replaceWith(T replacement) {
            this.replacement = replacement;
            return this;
        }

        protected abstract String formatSummary(String subject);

        protected abstract String formatAdvice(T replacement);

        protected String removalDetails() {
            return thisIsScheduledToBeRemoved();
        }

        @Override
        DeprecationMessage build() {
            withSummary(formatSummary(subject));
            withRemovalDetails(removalDetails());
            if (replacement != null) {
                withAdvice(formatAdvice(replacement));
            }
            return super.build();
        }
    }

    // Output: ${summary}. This has been deprecated and is scheduled to be removed in Gradle {X}.
    public static DeprecationMessage.Builder thisHasBeenDeprecated(final String summary) {
        return new DeprecationMessage.Builder() {
            @Override
            DeprecationMessage build() {
                withSummary(summary);
                withRemovalDetails(thisHasBeenDeprecatedAndIsScheduledToBeRemoved());
                return super.build();
            }
        };
    }

    // Output: ${thing} has been deprecated. This is scheduled to be removed in Gradle {X}.
    public static DeprecationMessage.Builder specificThingHasBeenDeprecated(final String thing) {
        return new DeprecationMessage.Builder() {
            @Override
            DeprecationMessage build() {
                withSummary(xHasBeenDeprecated(thing));
                withRemovalDetails(thisIsScheduledToBeRemoved());
                return super.build();
            }
        };
    }

    // Output: ${thing} has been deprecated. This is scheduled to be removed in Gradle {X}.
    public static DeprecationMessage.Builder indirectCodeUsageHasBeenDeprecated(String thing) {
        return specificThingHasBeenDeprecated(thing).withIndirectUsage();
    }

    // Output: ${feature} has been deprecated. This is scheduled to be removed in Gradle {X}.
    public static DeprecationMessage.Builder deprecatedBuildInvocationFeature(String feature) {
        return specificThingHasBeenDeprecated(feature).withBuildInvocation();
    }

    // Output: ${behaviour}. This behaviour has been deprecated and is scheduled to be removed in Gradle {X}.
    public static DeprecationMessage.Builder behaviourHasBeenDeprecated(final String behaviour) {
        return new DeprecationMessage.Builder() {
            @Override
            DeprecationMessage build() {
                withSummary(behaviour);
                withRemovalDetails(thisBehaviourHasBeenDeprecatedAndIsScheduledToBeRemoved());
                return super.build();
            }
        };
    }

    public static class DeprecateNamedParameterBuilder extends DeprecationWithReplacementBuilder<String> {

        DeprecateNamedParameterBuilder(String parameter) {
            super(parameter);
        }

        @Override
        protected String formatSummary(String parameter) {
            return String.format("The %s named parameter has been deprecated.", parameter);
        }

        @Override
        protected String formatAdvice(String replacement) {
            return String.format("Please use the %s named parameter instead.", replacement);
        }
    }

    public static class DeprecatePropertyBuilder extends DeprecationWithReplacementBuilder<String> {

        DeprecatePropertyBuilder(String property) {
            super(property);
        }

        @Override
        protected String formatSummary(String property) {
            return String.format("The %s property has been deprecated.", property);
        }

        @Override
        protected String formatAdvice(String replacement) {
            return String.format("Please use the %s property instead.", replacement);
        }
    }

    public static class DeprecateConfigurationBuilder extends DeprecationWithReplacementBuilder<List<String>> {
        private final ConfigurationDeprecationType deprecationType;

        DeprecateConfigurationBuilder(String configuration, ConfigurationDeprecationType deprecationType) {
            super(configuration);
            this.deprecationType = deprecationType;
            if (!deprecationType.inUserCode) {
                withIndirectUsage();
            }
        }

        @Override
        protected String formatSummary(String configuration) {
            return String.format("The %s configuration has been deprecated for %s.", configuration, deprecationType.displayName());
        }

        @Override
        protected String formatAdvice(List<String> replacements) {
            return String.format("Please %s the %s configuration instead.", deprecationType.usage, Joiner.on(" or ").join(replacements));
        }

        @Override
        protected String removalDetails() {
            return thisWillBecomeAnError();
        }
    }

    public static class DeprecateMethodBuilder extends DeprecationWithReplacementBuilder<String> {

        DeprecateMethodBuilder(String method) {
            super(method);
        }

        @Override
        protected String formatSummary(String method) {
            return String.format("The %s method has been deprecated.", method);
        }

        @Override
        protected String formatAdvice(String replacement) {
            return pleaseUseThisMethodInstead(replacement);
        }
    }

    public static class DeprecateInvocationBuilder extends DeprecationWithReplacementBuilder<String> {

        DeprecateInvocationBuilder(String invocation) {
            super(invocation);
        }

        @Override
        protected String formatSummary(String invocation) {
            return String.format("Using method %s has been deprecated.", invocation);
        }

        @Override
        protected String formatAdvice(String replacement) {
            return pleaseUseThisMethodInstead(replacement);
        }

        @Override
        protected String removalDetails() {
            return thisWillBecomeAnError();
        }
    }

    // Use for some operation that is not deprecated, but something about the method parameters or state is deprecated.
    // Output: ${invocation} has been deprecated. This will fail with an error in Gradle {X}.
    public static DeprecationMessage.Builder discontinuedInvocation(final String invocation) {
        return new DeprecationMessage.Builder() {
            @Override
            DeprecationMessage build() {
                withSummary(xHasBeenDeprecated(invocation));
                withRemovalDetails(thisWillBecomeAnError());
                return super.build();
            }
        };
    }

    public static class DeprecateTaskBuilder extends DeprecationWithReplacementBuilder<String> {
        DeprecateTaskBuilder(String task) {
            super(task);
        }

        @Override
        protected String formatSummary(String task) {
            return String.format("The %s task has been deprecated.", task);
        }

        @Override
        protected String formatAdvice(String replacement) {
            return String.format("Please use the %s task instead.", replacement);
        }
    }

    public static class DeprecatePluginBuilder extends DeprecationWithReplacementBuilder<String> {

        private boolean externalReplacement = false;

        DeprecatePluginBuilder(String plugin) {
            super(plugin);
        }

        @Override
        protected String formatSummary(String plugin) {
            return String.format("The %s plugin has been deprecated.", plugin);
        }

        @Override
        protected String formatAdvice(String replacement) {
            return externalReplacement ? String.format("Consider using the %s plugin instead.", replacement) : String.format("Please use the %s plugin instead.", replacement);
        }

        public Builder replaceWithExternalPlugin(String replacement) {
            this.externalReplacement = true;
            return replaceWith(replacement);
        }

        public Builder withUpgradeGuideSection(int majorVersion, String upgradeGuideSection) {
            // TODO: this is how it works with current implementation. Start here with extracting deprecation documentation model
            return withAdvice("Consult the upgrading guide for further information: " + DOCUMENTATION_REGISTRY.getDocumentationFor("upgrading_version_" + majorVersion, upgradeGuideSection));
        }
    }

    public DeprecatedFeatureUsage toDeprecatedFeatureUsage(Class<?> calledFrom) {
        return new DeprecatedFeatureUsage(summary, removalDetails, advice, contextualAdvice, usageType, calledFrom);
    }

}
