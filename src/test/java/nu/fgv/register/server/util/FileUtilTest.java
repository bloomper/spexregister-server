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

package nu.fgv.register.server.util;

import nu.fgv.register.server.util.error.BadRequestException;
import org.junit.jupiter.api.Test;
import org.springframework.util.ResourceUtils;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author Anders Jacobsson
 * @since 2.0
 */
class FileUtilTest {

    @Test
    void should_detect_image_type_from_content() throws Exception {
        final byte[] png = Files.readAllBytes(ResourceUtils.getFile("classpath:test.png").toPath());

        assertThat(FileUtil.requireSupportedImageMimeType(png), is("image/png"));
    }

    @Test
    void should_reject_html_content() {
        final byte[] html = "<html><body><script>alert(1)</script></body></html>".getBytes(StandardCharsets.UTF_8);

        assertThrows(BadRequestException.class, () -> FileUtil.requireSupportedImageMimeType(html));
    }

    @Test
    void should_reject_svg_content() {
        final byte[] svg = "<?xml version=\"1.0\"?><svg xmlns=\"http://www.w3.org/2000/svg\" onload=\"alert(1)\"/>".getBytes(StandardCharsets.UTF_8);

        assertThrows(BadRequestException.class, () -> FileUtil.requireSupportedImageMimeType(svg));
    }

    @Test
    void should_reject_empty_content() {
        assertThrows(BadRequestException.class, () -> FileUtil.requireSupportedImageMimeType(new byte[0]));
    }
}
