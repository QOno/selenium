package org.openqa.selenium.remote.tracing;

import io.opentracing.SpanContext;
import io.opentracing.Tracer;
import io.opentracing.propagation.Format;
import io.opentracing.propagation.TextMap;
import io.opentracing.propagation.TextMapExtractAdapter;
import io.opentracing.util.GlobalTracer;

import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;

class OpenTracingTracer implements DistributedTracer {

  private final Tracer delegate;

  public OpenTracingTracer(Tracer delegate) {
    this.delegate = Objects.requireNonNull(delegate);
  }

  @Override
  public Span createSpan(String operation, Span parent) {
    SpanContext context = null;
    if (parent instanceof OpenTracingSpan) {
      context = ((OpenTracingSpan) parent).getContext();
    }

    io.opentracing.Span span = delegate.buildSpan(operation).asChildOf(context).start();
    delegate.scopeManager().activate(span, false);
    OpenTracingSpan toReturn = new OpenTracingSpan(delegate, span);
    toReturn.activate();
    return toReturn;
  }

  @Override
  public <C> Span createSpan(
      String operationName,
      C carrier,
      Function<C, Map<String, String>> extractor) {
    Map<String, String> map = extractor.apply(carrier);

    SpanContext context = delegate.extract(
        Format.Builtin.HTTP_HEADERS,
        new TextMapExtractAdapter(map));

    io.opentracing.Span span = delegate.buildSpan(operationName).asChildOf(context).start();
    delegate.scopeManager().activate(span, false);
    OpenTracingSpan toReturn = new OpenTracingSpan(delegate, span);
    toReturn.activate();
    return toReturn;
  }

  @Override
  public Span getActiveSpan() {
    io.opentracing.Span span = delegate.activeSpan();
    return span == null ? null : new OpenTracingSpan(delegate, span);
  }
}
