/*
 * Copyright 2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package nu.fgv.register.server.util.search;

import org.hibernate.search.backend.lucene.analysis.LuceneAnalysisConfigurationContext;
import org.hibernate.search.backend.lucene.analysis.LuceneAnalysisConfigurer;
import org.hibernate.search.engine.backend.analysis.AnalyzerNames;

/**
 * @author Anders Jacobsson
 * @since 2.0
 */
public class DefaultOverridingLuceneAnalysisConfigurer implements LuceneAnalysisConfigurer {
    public static final String NORMALIZER_LOWERCASE = "lowercase";

    @Override
    public void configure(final LuceneAnalysisConfigurationContext context) {
        context.normalizer(NORMALIZER_LOWERCASE).custom()
                .tokenFilter("lowercase")
                .tokenFilter("asciiFolding");

        context.analyzer(AnalyzerNames.DEFAULT).custom()
                .tokenizer("standard")
                .tokenFilter("lowercase")
                .tokenFilter("asciiFolding")
                .tokenFilter("snowballPorter")
                .param("language", "Swedish")
                .tokenFilter("asciiFolding");
    }
}
