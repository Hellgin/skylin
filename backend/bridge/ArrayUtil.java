package skylin.bridge;

import java.lang.reflect.Array;
import java.util.Iterator;
import java.util.NoSuchElementException;

public class ArrayUtil {
	public static Iterable<Object> toIterable(final Object array) {
		if (array == null) {
			return emptyIterable();
		}

		if (!array.getClass().isArray()) {
			throw new IllegalArgumentException("object not of type array, was " + array.getClass().getName());
		}

		if (Array.getLength(array) == 0) {
			return emptyIterable();
		}

		return new Iterable<Object>() {

			@Override
			public Iterator<Object> iterator() {
				return new Iterator<Object>() {

					private int _i = 0;
					private final int _length = Array.getLength(array);

					@Override
					public boolean hasNext() {
						return _i < _length;
					}

					@Override
					public Object next() {
						return Array.get(array, _i++);
					}

					@Override
					public void remove() {
						throw new UnsupportedOperationException();
					}
				};
			}
		};
	}
	
	public static <T> Iterable<T> emptyIterable() {
		return (Iterable<T>) EMPTY_ITERABLE;
	}
	
	public static final Iterable<?> EMPTY_ITERABLE = new Iterable<Object>() {

		@Override
		public Iterator<Object> iterator() {
			return empty();
		}
	};
	
	@SuppressWarnings("unchecked")
	public static <T> Iterator<T> empty() {
		return (Iterator<T>) EMPTY_ITERATOR;
	}
	
	public static final Iterator<?> EMPTY_ITERATOR = new Iterator<Object>() {

		@Override
		public boolean hasNext() {
			return false;
		}

		@Override
		public Object next() {
			throw new NoSuchElementException();
		}

		@Override
		public void remove() {
			throw new IllegalStateException();
		}

	};
}
