/*
 * Copyright 2018 GoDataDriven B.V.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.divolte.server.config;

import javax.annotation.ParametersAreNonnullByDefault;
import javax.annotation.ParametersAreNullableByDefault;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;

import java.time.Duration;
import java.util.Optional;

@ParametersAreNonnullByDefault
public final class JavascriptConfiguration {
    private static final String DEFAULT_NAME = "divolte.js";
    private static final String DEFAULT_FILE = "divolte.js";
    private static final String DEFAULT_LOGGING = "false";
    private static final String DEFAULT_DEBUG = "false";
    private static final String DEFAULT_AUTO_PAGE_VIEW_EVENT = "true";
    private static final String DEFAULT_EVENT_TIMEOUT = "750 milliseconds";

    static final JavascriptConfiguration DEFAULT_JAVASCRIPT_CONFIGURATION =
            new JavascriptConfiguration(DEFAULT_NAME,
                                        DEFAULT_FILE,
                                        Boolean.parseBoolean(DEFAULT_LOGGING),
                                        Boolean.parseBoolean(DEFAULT_DEBUG),
                                        Boolean.parseBoolean(DEFAULT_AUTO_PAGE_VIEW_EVENT),
                                        DurationDeserializer.parseDuration(DEFAULT_EVENT_TIMEOUT));

    @NotNull @NotEmpty @Pattern(regexp="^[A-Za-z0-9_-]+\\.js$")
    public final String name;

    @NotNull @NotEmpty
    public final String file;

    public final boolean logging;
    public final boolean debug;
    public final boolean autoPageViewEvent;
    public final Duration eventTimeout;

    @JsonCreator
    @ParametersAreNullableByDefault
    JavascriptConfiguration(@JsonProperty(defaultValue=DEFAULT_NAME) final String name,
                            @JsonProperty(defaultValue=DEFAULT_FILE) final String file,
                            @JsonProperty(defaultValue=DEFAULT_LOGGING) final Boolean logging,
                            @JsonProperty(defaultValue=DEFAULT_DEBUG) final Boolean debug,
                            @JsonProperty(defaultValue=DEFAULT_AUTO_PAGE_VIEW_EVENT) final Boolean autoPageViewEvent,
                            @JsonProperty(defaultValue=DEFAULT_EVENT_TIMEOUT) final Duration eventTimeout) {
        // TODO: register a custom deserializer with Jackson that uses the defaultValue property from the annotation to fix this
        this.name = Optional.ofNullable(name).orElse(DEFAULT_NAME);
        this.file = Optional.ofNullable(file).orElse(DEFAULT_FILE);
        this.logging = Optional.ofNullable(logging).orElseGet(() -> Boolean.valueOf(DEFAULT_LOGGING));
        this.debug = Optional.ofNullable(debug).orElseGet(() -> Boolean.valueOf(DEFAULT_DEBUG));
        this.autoPageViewEvent = Optional.ofNullable(autoPageViewEvent).orElseGet(() -> Boolean.valueOf(DEFAULT_AUTO_PAGE_VIEW_EVENT));
        this.eventTimeout = Optional.ofNullable(eventTimeout).orElseGet(() -> DurationDeserializer.parseDuration(DEFAULT_EVENT_TIMEOUT));
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("name", name)
                .add("file", file)
                .add("logging", logging)
                .add("debug", debug)
                .add("autoPageViewEvent", autoPageViewEvent)
                .add("eventTimeout", eventTimeout)
                .toString();
    }
}
