package de.webis.ehcache3diskpersistencedemo;

import java.util.function.Function;

import org.ehcache.Cache;
import org.ehcache.PersistentCacheManager;
import org.ehcache.config.builders.CacheConfigurationBuilder;
import org.ehcache.config.builders.CacheManagerBuilder;
import org.ehcache.config.builders.ResourcePoolsBuilder;
import org.ehcache.config.units.MemoryUnit;

public class Ehcache3DiskPersistenceDemo {
	final static PersistentCacheManager cacheManager = CacheManagerBuilder.newCacheManagerBuilder()
			.with(CacheManagerBuilder.persistence(".ehcache")).build();
	// TODO specify the path on your system
	static {
		cacheManager.init();

		// Disk Cache is only persistent when the cacheManager shuts down correctly
		Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
			@Override
			public void run() {
				cacheManager.close();
			}
		}));
	}

	public static <F, T> Function<F, T> memoize(final Function<F, T> inputFunction, Class<F> f, Class<T> t,
			String cachename) {

		return new Function<F, T>() {
			Cache<F, T> c = cacheManager.getCache(cachename, f, t);
			Cache<F, T> memoization = c != null ? c
					: cacheManager.createCache(cachename, CacheConfigurationBuilder.newCacheConfigurationBuilder(f, t,
							ResourcePoolsBuilder.heap(1000).disk(100, MemoryUnit.GB, true)));
			// Above configuration lets you put 1000 elements on the heap and 100 GB to the
			// disk

			@Override
			public T apply(final F input) {
				if (!memoization.containsKey(input)) {
					memoization.put(input, inputFunction.apply(input));
				}
				return memoization.get(input);
			}
		};
	}

	public Ehcache3DiskPersistenceDemo() {
		Function<Integer, Integer> memMethodToCache = memoize(this::methodToCache, Integer.class, Integer.class,
				"methodToCache");
		Function<Integer, Integer> memMethod2ToCache = memoize(this::method2ToCache, Integer.class, Integer.class,
						"method2ToCache");
		for (int i = 0; i < 50; i++) {
			long ti = System.currentTimeMillis();
			System.out.print(memMethodToCache.apply(i) + ":");
			System.out.println(System.currentTimeMillis() - ti);
		}
		for (int i = 0; i < 50; i++) {
			long ti = System.currentTimeMillis();
			System.out.print(memMethod2ToCache.apply(i) + ":");
			System.out.println(System.currentTimeMillis() - ti);
		}
	}

	public static void main(String[] args) {
		new Ehcache3DiskPersistenceDemo();
	}

	public int methodToCache(int i) {
		try {
			Thread.sleep(200);
		} catch (InterruptedException e) {
			throw new RuntimeException();
		}
		return i + 2;
	}
	
	public int method2ToCache(int i) {
		try {
			Thread.sleep(200);
		} catch (InterruptedException e) {
			throw new RuntimeException();
		}
		return i * 2;
	}
}
