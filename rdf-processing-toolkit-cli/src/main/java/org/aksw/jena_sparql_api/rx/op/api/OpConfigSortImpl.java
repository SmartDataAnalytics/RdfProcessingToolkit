package org.aksw.jena_sparql_api.rx.op.api;

import java.nio.file.Path;
import java.util.function.Function;

import org.aksw.jena_sparql_api.utils.model.ResourceInDataset;

import io.reactivex.rxjava3.core.FlowableTransformer;

public class OpConfigSortImpl<T, K>
    implements OpConfigSort<T, K>
{
    protected Path temporaryDirectory;
    protected Function<? super T, K> keyFn;
    protected Long bufferSize;
    protected Integer parallel;
    protected boolean randomSort;
    protected boolean unique;
    protected boolean mergeConsecutiveItems;
    protected boolean reverseSortOrder;

    protected Object keySerializer;
    protected Object itemSerializer;

    protected Object keyDeserializer;
    protected Object itemDeserializer;

    @Override
    public OpConfigSort<T, K> setTemporaryDirectory(Path path) {
        this.temporaryDirectory = path;
        return this;
    }

    @Override
    public Path getTemporaryDirectory() {
        return temporaryDirectory;
    }

    @Override
    public Function<? super T, K> getKeyFn() {
        return keyFn;
    }

    @Override
    public OpConfigSort<T, K> setKeyFn(Function<ResourceInDataset, ?> keyFn) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public OpConfigSort<T, K> setRandomSort(boolean onOrOff) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public OpConfigSort<T, K> setReverse(boolean onOrOff) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public OpConfigSort<T, K> setUnique(boolean onOrOff) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public OpConfigSort<T, K> setBufferSize(long sizeInBytes) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public OpConfigSort<T, K> setParallel(int parallel) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public OpConfigSort<T, K> mergeConsecutiveRecords(boolean onOrOff) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public FlowableTransformer<T, T> get() {
        // TODO Auto-generated method stub
        return null;
    }

}
