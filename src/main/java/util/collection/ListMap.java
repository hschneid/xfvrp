package util.collection;

import util.Copyable;

import java.util.*;

/** 
 * Copyright (c) 2012-present Holger Schneider
 * All rights reserved.
 *
 * This source code is licensed under the MIT License (MIT) found in the
 * LICENSE file in the root directory of this source tree.
 *
 * 
 * @author hschneid
 *
 */
public class ListMap<K, E> implements Copyable<ListMap<K,E>> {
	
	private static final int DEFAULT_LIST_SIZE = 10;
	private final HashMap<K, List<E>> map;
	
	private ListMap() {
		map = new HashMap<>();
	}
	
	private ListMap(int size) {
		map = new HashMap<>(size);
	}
	
	public static <K,E> ListMap<K,E> create() {
		return new ListMap<>();
	}
	
	public static <K,E> ListMap<K,E> create(int size) {
		return new ListMap<>(size);
	}
	
	public void put(K key, E element) {
		List<E> list = map.get(key);
		if(list != null) {
			map.get(key).add(element);
		} else {
			list = createList(DEFAULT_LIST_SIZE);
			list.add(element);
			map.put(key, list);
		}
	}
	
	public void putAll(K key, List<E> list) {
		if(map.containsKey(key))
			map.get(key).addAll(list);
		else
			map.put(key, list);
	}
	
	public void remove(K key) {
		map.remove(key);
	}
	
	public void remove(K key, E element) {
		if(map.containsKey(key))
			map.get(key).remove(element);
	}
	
	public List<E> get(K key) {
		return map.get(key);
	}
	
	public boolean containsKey(K key) {
		return map.containsKey(key);
	}

	public Set<K> keySet() {
		return map.keySet();
	}
	
	public Collection<List<E>> values() {
		return map.values();
	}
	
	public int size() {
		return map.size();
	}
	
	public void clear() {
		map.clear();
	}
	
	public Iterator<E> iterator(K key) {
		if(map.containsKey(key))
			return map.get(key).iterator();
		
		return null;
	}

	/*
	 * (non-Javadoc)
	 * @see da.util.Copyable#copy()
	 */
	@Override
	public ListMap<K, E> copy() {
		ListMap<K, E> copy = new ListMap<>(size());
		
		for (K key : keySet()) {
			List<E> list = get(key);
			if(list != null) {
				List<E> newList = createList(list.size());
				newList.addAll(list);
				copy.putAll(key, newList);
			} else {
				copy.put(key, null);
			}
		}
		
		return copy;
	}
	
	private List<E> createList(int size) {
		return new ArrayList<>(size);
	}
}
