package org.crayne.mu.runtime.util.errorhandler;

import org.crayne.mu.runtime.util.LimitedSizeQueue;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class Traceback {

    public static final int TRACEBACK_MAX_LENGTH = 32;
    final LimitedSizeQueue<TracebackElement> tracebackElements;

    public Traceback() {
        this.tracebackElements = new LimitedSizeQueue<>(TRACEBACK_MAX_LENGTH);
    }

    public void add(@NotNull final TracebackElement elem) {
        tracebackElements.add(elem);
    }

    public LimitedSizeQueue<TracebackElement> getTracebackElements() {
        return tracebackElements;
    }

    public String toString() {
        final List<String> asStringList = new ArrayList<>(
                tracebackElements
                .stream()
                .map(TracebackElement::toString)
                .filter(Objects::nonNull)
                .map(s -> " at " + s).toList()
        );
        Collections.reverse(asStringList);

        return "Mu-Lang Traceback helper\n" + (
                String.join("\n", asStringList)
        ).indent(4);
    }

}
