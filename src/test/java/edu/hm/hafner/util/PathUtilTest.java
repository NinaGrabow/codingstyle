package edu.hm.hafner.util;

import java.io.IOException;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import static org.assertj.core.api.Assertions.*;
import static org.assertj.core.api.Assumptions.*;

/**
 * Tests the class {@link PathUtil}.
 *
 * @author Ullrich Hafner
 */
@SuppressFBWarnings("DMI")
class PathUtilTest extends ResourceTest {
    private static final String NOT_EXISTING = "/should/not/exist";
    private static final String ILLEGAL = "\0 Null-Byte";
    private static final String FILE_NAME = "relative.txt";
    private static final String NOT_EXISTING_RELATIVE = "not-existing-relative";

    /**
     * Ensures that illegal file names are processed without problems and the test for existence returns {@code false}.
     */
    @ParameterizedTest(name = "[{index}] Illegal filename = {0}")
    @ValueSource(strings = {"/does/not/exist", "!<>$&/&(", "\0 Null-Byte", "C:/!<>$&/&( \0", "/!<>$&/&( \0"})
    @DisplayName("Should not change path on errors")
    void shouldReturnFallbackOnError(final String fileName) {
        PathUtil pathUtil = new PathUtil();

        assertThat(pathUtil.exists(fileName)).isFalse();
    }

    @DisplayName("Should find some files in the resources folder")
    @Test
    void shouldFindResourceFolder() {
        PathUtil pathUtil = new PathUtil();

        assertThat(pathUtil.exists(getResourceAsFile(FILE_NAME).toString())).isTrue();
        assertThat(pathUtil.exists(getResourceAsFile(FILE_NAME).getParent().toString())).isTrue();
        assertThat(pathUtil.exists(FILE_NAME, getResourceAsFile(FILE_NAME).getParent().toString())).isTrue();
        assertThat(pathUtil.exists(getResourceAsFile(FILE_NAME).getRoot().toString())).isTrue();
    }

    @DisplayName("Should be absolute path")
    @ParameterizedTest(name = "[{index}] path={0}")
    @ValueSource(strings = {"/", "/tmp", "C:\\", "C:\\Tmp"})
    void shouldFindAbsolutePaths(final String path) {
        PathUtil pathUtil = new PathUtil();

        assertThat(pathUtil.isAbsolute(path)).isTrue();
    }

    @Test
    void shouldReturnFallback() {
        PathUtil pathUtil = new PathUtil();

        assertThat(pathUtil.getAbsolutePath(NOT_EXISTING)).isEqualTo(NOT_EXISTING);
        assertThat(pathUtil.getAbsolutePath("C:\\should\\not\\exist")).isEqualTo("C:" + NOT_EXISTING);
        assertThat(pathUtil.getAbsolutePath(ILLEGAL)).isEqualTo(ILLEGAL);
    }

    @Test
    void shouldConvertToAbsolute() {
        PathUtil pathUtil = new PathUtil();

        assertThat(pathUtil.createAbsolutePath(null, FILE_NAME)).isEqualTo(FILE_NAME);
        assertThat(pathUtil.createAbsolutePath("", FILE_NAME)).isEqualTo(FILE_NAME);
        assertThat(pathUtil.createAbsolutePath("/", FILE_NAME)).isEqualTo("/" + FILE_NAME);
        assertThat(pathUtil.createAbsolutePath("/tmp", FILE_NAME)).isEqualTo("/tmp/" + FILE_NAME);
        assertThat(pathUtil.createAbsolutePath("/tmp/", FILE_NAME)).isEqualTo("/tmp/" + FILE_NAME);
    }

    @Test
    void shouldConvertToRelative() {
        PathUtil pathUtil = new PathUtil();

        Path absolutePath = getResourceAsFile(FILE_NAME);

        assertThat(pathUtil.getRelativePath(absolutePath.getParent(), FILE_NAME)).isEqualTo(FILE_NAME);
        assertThat(pathUtil.getRelativePath(absolutePath.getParent(), NOT_EXISTING_RELATIVE)).isEqualTo(NOT_EXISTING_RELATIVE);

        assertThat(pathUtil.getRelativePath(absolutePath.getParent().getParent(), "util/" + FILE_NAME)).isEqualTo("util/" + FILE_NAME);

        assertThat(pathUtil.getRelativePath(absolutePath.getParent(), absolutePath.toString())).isEqualTo(FILE_NAME);
        assertThat(pathUtil.getRelativePath(Paths.get(NOT_EXISTING), absolutePath.toString())).isEqualTo(pathUtil.getAbsolutePath(absolutePath));
        assertThat(pathUtil.getRelativePath(Paths.get(NOT_EXISTING), FILE_NAME)).isEqualTo(FILE_NAME);
    }

    @Test
    void shouldConvertNotResolvedToRelative() {
        PathUtil pathUtil = new PathUtil();

        Path absolutePath = getResourceAsFile(FILE_NAME);

        assertThat(pathUtil.getRelativePath(absolutePath.getParent().getParent(), "./util/" + FILE_NAME)).isEqualTo("util/" + FILE_NAME);
        assertThat(pathUtil.getRelativePath(absolutePath.getParent().getParent(), "../hafner/util/" + FILE_NAME)).isEqualTo("util/" + FILE_NAME);
    }

    @Test
    void shouldSkipAlreadyAbsoluteOnUnix() {
        assumeThatTestIsRunningOnUnix();

        PathUtil pathUtil = new PathUtil();

        assertThat(pathUtil.createAbsolutePath("/tmp/", "/tmp/file.txt")).isEqualTo("/tmp/file.txt");
    }

    @Test
    void shouldSkipAlreadyAbsoluteOnWindows() {
        assumeThatTestIsRunningOnWindows();

        PathUtil pathUtil = new PathUtil();

        assertThat(pathUtil.createAbsolutePath("C:\\tmp", "C:\\tmp\\file.txt")).isEqualTo("C:/tmp/file.txt");
    }

    @Test
    void shouldNormalizeDriveLetter() {
        PathUtil pathUtil = new PathUtil();

        assertThat(pathUtil.getAbsolutePath("c:\\tmp")).isEqualTo("C:/tmp");
    }

    @Test
    void shouldStayInSymbolicLinks() throws IOException {
        Path current = Paths.get(".");
        Path real = current.toRealPath();
        Path realWithSymbolic = current.toRealPath(LinkOption.NOFOLLOW_LINKS);
        assumeThat(real).isNotEqualTo(realWithSymbolic);

        String fromUtil = new PathUtil().getAbsolutePath(current);
        String unixStyle = realWithSymbolic.toString().replace('\\', '/');
        assertThat(fromUtil).isEqualTo(unixStyle);
    }
}
