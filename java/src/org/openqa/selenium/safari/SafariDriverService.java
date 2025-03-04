// Licensed to the Software Freedom Conservancy (SFC) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The SFC licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.

package org.openqa.selenium.safari;

import com.google.auto.service.AutoService;

import org.openqa.selenium.Capabilities;
import org.openqa.selenium.Platform;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.net.PortProber;
import org.openqa.selenium.remote.service.DriverService;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Collections.unmodifiableList;
import static java.util.Collections.unmodifiableMap;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.openqa.selenium.Platform.MAC;
import static org.openqa.selenium.remote.Browser.SAFARI;

public class SafariDriverService extends DriverService {

  /**
   * System property that defines the location of the safaridriver executable that will be used by
   * the {@link #createDefaultService() default service}.
   */
  public static final String SAFARI_DRIVER_EXE_PROPERTY = "webdriver.safari.driver";

  private static final File SAFARI_DRIVER_EXECUTABLE = new File("/usr/bin/safaridriver");

  public SafariDriverService(
    File executable,
    int port,
    List<String> args,
    Map<String, String> environment) throws IOException {
    super(executable, port, DEFAULT_TIMEOUT,
      unmodifiableList(new ArrayList<>(args)),
      unmodifiableMap(new HashMap<>(environment)));
  }

  public SafariDriverService(
    File executable,
    int port,
    Duration timeout,
    List<String> args,
    Map<String, String> environment) throws IOException {
    super(executable, port, timeout,
          unmodifiableList(new ArrayList<>(args)),
          unmodifiableMap(new HashMap<>(environment)));
  }

  public static SafariDriverService createDefaultService() {
    return new Builder().build();
  }

  /**
   * Checks if the browser driver binary is available. Grid uses this method to show
   * the available browsers and drivers, hence its visibility.
   *
   * @return Whether the browser driver path was found.
   */
  static boolean isPresent() {
    return findExePath(SAFARI_DRIVER_EXECUTABLE.getAbsolutePath(),
                       SAFARI_DRIVER_EXE_PROPERTY) != null;
  }

  @Override
  protected void waitUntilAvailable() {
    try {
      PortProber.waitForPortUp(getUrl().getPort(), (int) getTimeout().toMillis(), MILLISECONDS);
    } catch (RuntimeException e) {
      throw new WebDriverException(e);
    }
  }

  @AutoService(DriverService.Builder.class)
  public static class Builder extends DriverService.Builder<
      SafariDriverService, SafariDriverService.Builder> {

    @Override
    public int score(Capabilities capabilities) {
      int score = 0;

      if (SAFARI.is(capabilities)) {
        score++;
      }

      return score;
    }

    @Override
    protected File findDefaultExecutable() {
      File exe;
      if (System.getProperty(SAFARI_DRIVER_EXE_PROPERTY) != null) {
        exe = new File(System.getProperty(SAFARI_DRIVER_EXE_PROPERTY));
      } else {
        exe = SAFARI_DRIVER_EXECUTABLE;
      }

      if (!exe.isFile()) {
        StringBuilder message = new StringBuilder();
        message.append("Unable to find driver executable: ").append(exe);

        if (!isElCapitanOrLater()) {
          message.append("(SafariDriver requires Safari 10 running on OSX El Capitan or greater.)");
        }
        throw new WebDriverException(message.toString());
      }

      return exe;
    }

    private boolean isElCapitanOrLater() {
      if (!Platform.getCurrent().is(MAC)) {
        return false;
      }

      // If we're using a version of macOS later than 10, we're set.
      if (Platform.getCurrent().getMajorVersion() > 10) {
        return true;
      }

      // Check the El Capitan version
      return Platform.getCurrent().getMajorVersion() == 10 &&
             Platform.getCurrent().getMinorVersion() >= 11;
    }

    @Override
    protected List<String> createArgs() {
      return Arrays.asList("--port", String.valueOf(getPort()));
    }

    @Override
    protected SafariDriverService createDriverService(
        File exe,
        int port,
        Duration timeout,
        List<String> args,
        Map<String, String> environment) {
      try {
        return new SafariDriverService(exe, port, timeout, args, environment);
      } catch (IOException e) {
        throw new WebDriverException(e);
      }
    }
  }
}
