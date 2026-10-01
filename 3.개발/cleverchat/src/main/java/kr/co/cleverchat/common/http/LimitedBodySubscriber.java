package kr.co.cleverchat.common.http;

import java.io.IOException;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Flow;

/** Caps the response while receiving it, before the byte-array subscriber can grow unbounded. */
public final class LimitedBodySubscriber implements HttpResponse.BodySubscriber<byte[]> {
    private final HttpResponse.BodySubscriber<byte[]> delegate =
            HttpResponse.BodySubscribers.ofByteArray();
    private final long limit;
    private final String message;
    private Flow.Subscription subscription;
    private long received;
    private boolean failed;

    public LimitedBodySubscriber(long limit, String message) {
        this.limit = limit;
        this.message = message;
    }

    @Override
    public CompletionStage<byte[]> getBody() {
        return delegate.getBody();
    }

    @Override
    public void onSubscribe(Flow.Subscription value) {
        subscription = value;
        delegate.onSubscribe(value);
    }

    @Override
    public void onNext(List<ByteBuffer> buffers) {
        if (failed) return;
        for (ByteBuffer buffer : buffers) received += buffer.remaining();
        if (received > limit) {
            failed = true;
            subscription.cancel();
            delegate.onError(new IOException(message));
        } else delegate.onNext(buffers);
    }

    @Override
    public void onError(Throwable error) {
        if (!failed) delegate.onError(error);
    }

    @Override
    public void onComplete() {
        if (!failed) delegate.onComplete();
    }
}
