package no.companyfetcher.gui;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.function.Consumer;

final class GuiLogBridge implements AutoCloseable {

    private static final String APPENDER_NAME = "GUI_LOG";
    private static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern("HH:mm:ss").withZone(ZoneId.systemDefault());

    private final GuiLogAppender appender;

    private GuiLogBridge(Consumer<String> logConsumer) {
        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        appender = new GuiLogAppender(logConsumer);
        appender.setContext(context);
        appender.setName(APPENDER_NAME);
        appender.start();

        ch.qos.logback.classic.Logger root = context.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
        root.addAppender(appender);
    }

    static GuiLogBridge attach(Consumer<String> logConsumer) {
        return new GuiLogBridge(logConsumer);
    }

    @Override
    public void close() {
        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        ch.qos.logback.classic.Logger root = context.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
        root.detachAppender(appender);
        appender.stop();
    }

    private static final class GuiLogAppender extends AppenderBase<ILoggingEvent> {

        private final Consumer<String> consumer;

        private GuiLogAppender(Consumer<String> consumer) {
            this.consumer = consumer;
        }

        @Override
        protected void append(ILoggingEvent event) {
            if (!isStarted()) {
                return;
            }

            String time = TIME_FORMAT.format(Instant.ofEpochMilli(event.getTimeStamp()));
            String line = "[" + time + "] " + event.getFormattedMessage();
            consumer.accept(line);
        }
    }
}
