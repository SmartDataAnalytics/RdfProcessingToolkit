package org.aksw.jena_sparql_api.rx.op.api;

import java.nio.file.Path;
import java.util.function.Function;

import org.aksw.jena_sparql_api.utils.model.ResourceInDataset;

import io.reactivex.rxjava3.core.FlowableTransformer;

/**
 * Interface for common parameters of sort operations
 *
 * @author raven
 *
 */
public interface OpConfigSort<T, K> {
    OpConfigSort<T, K> setTemporaryDirectory(Path path);
    Path getTemporaryDirectory();

    Function<? super T, K> getKeyFn();
    OpConfigSort<T, K> setKeyFn(Function<ResourceInDataset, ?> keyFn);

    OpConfigSort<T, K> setRandomSort(boolean onOrOff);
    OpConfigSort<T, K> setReverse(boolean onOrOff);
    OpConfigSort<T, K> setUnique(boolean onOrOff);

    OpConfigSort<T, K> setBufferSize(long sizeInBytes);
    OpConfigSort<T, K> setParallel(int parallel);

    OpConfigSort<T, K> mergeConsecutiveRecords(boolean onOrOff);

    FlowableTransformer<T, T> get();
}
