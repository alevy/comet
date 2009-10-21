/*
 * Created on Jun 8, 2007
 * Created by Paul Gardner
 * Copyright (C) 2007 Aelitis, All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 * 
 * AELITIS, SAS au capital de 63.529,40 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 *
 */


package org.gudy.azureus2.core3.util;

import java.io.File;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import com.aelitis.azureus.core.util.HashCodeUtils;


public class 
StringInterner 
{
	private static final int SCHEDULED_CLEANUP_INTERVAL = 60*1000;
	
	private static final boolean TRACE_CLEANUP = false;
	private static final boolean TRACE_MULTIHITS = false;
	
	
	private static final int IMMEDIATE_CLEANUP_TRIGGER = 2000;
	private static final int IMMEDIATE_CLEANUP_GOAL = 1500;
	private static final int SCHEDULED_CLEANUP_TRIGGER = 1500;
	private static final int SCHEDULED_CLEANUP_GOAL = 1000;	
	private static final int SCHEDULED_AGING_THRESHOLD = 750;
	
	private static LightHashSet managedInterningSet = new LightHashSet(800);
	private static LightHashSet unmanagedInterningSet = new LightHashSet();
	
	private final static ReferenceQueue managedRefQueue = new ReferenceQueue();
	private final static ReferenceQueue unmanagedRefQueue = new ReferenceQueue();
	
	private static final String[] COMMON_KEYS = {
		"src","port","prot","ip","udpport","azver","httpport","downloaded",
		"Content","Refresh On","path.utf-8","uploaded","completed","persistent","attributes","encoding",
		"azureus_properties","stats.download.added.time","networks","p1","resume data","dndflags","blocks","resume",
		"primaryfile","resumecomplete","data","peersources","name.utf-8","valid","torrent filename","parameters",
		"secrets","timesincedl","tracker_cache","filedownloaded","timesinceul","tracker_peers","trackerclientextensions","GlobalRating",
		"comment.utf-8","Count","String","stats.counted","Thumbnail","Plugin.<internal>.DDBaseTTTorrent::sha1","type","Title",
		"displayname","Publisher","Creation Date","Revision Date","Content Hash","flags","stats.download.completed.time","Description",
		"Progressive","Content Type","QOS Class","DRM","hash","ver","id",
		"body","seed","eip","rid","iip","dp2","tp","orig",
		"dp","Quality","private","dht_backup_enable","max.uploads","filelinks","Speed Bps","cdn_properties",
		"sha1","ed2k","DRM Key","Plugin.aeseedingengine.attributes","initial_seed","dht_backup_requested","ta","size",
		"DIRECTOR PUBLISH","Plugin.azdirector.ContentMap","dateadded","bytesin","announces","status","bytesout","scrapes",
		"passive",
	};
	
	private static final ByteArrayHashMap	byte_map = new ByteArrayHashMap( COMMON_KEYS.length );
	
	static{
		try{
			for (int i=0;i<COMMON_KEYS.length;i++){
				
				byte_map.put( COMMON_KEYS[i].getBytes(Constants.BYTE_ENCODING), COMMON_KEYS[i] );
				managedInterningSet.add(new WeakStringEntry(COMMON_KEYS[i]));
			}
		}catch( Throwable e ){
			
			e.printStackTrace();
		}
		
			// initialisation nightmare - we have to create periodic event async to avoid
			// circular class loading issues when azureus.config is borkified
		
		new AEThread2( "asyncify", true )
		{
			public void
			run()
			{
				SimpleTimer.addPeriodicEvent("StringInterner:cleaner", SCHEDULED_CLEANUP_INTERVAL, new TimerEventPerformer() {
					public void perform(TimerEvent event) {
						sanitize(true);
						sanitizeLight();
					}
				});
			}
		}.start();
	}
		
	// private final static ReferenceQueue queue = new ReferenceQueue();

	
	public static String
	intern(
		byte[]	bytes )
	{
		String res = (String)byte_map.get( bytes );
		
		// System.out.println( new String( bytes ) + " -> " + res );
		
		return( res );
	}
	
	public static String intern(String toIntern) {
		
		if(toIntern == null)
			return null;
		
		String internedString;
		
		WeakStringEntry checkEntry = new WeakStringEntry(toIntern);
		
		synchronized( managedInterningSet ){

			sanitize(false);

			WeakStringEntry internedEntry = (WeakStringEntry) managedInterningSet.get(checkEntry);

			if (internedEntry == null || (internedString = internedEntry.getString()) == null)
			{
				internedString = toIntern;
				if(!managedInterningSet.add(checkEntry))
					System.out.println("unexpected modification");  // should not happen
			} else
			{
				internedEntry.incHits();
				checkEntry.destroy();
				if(TRACE_MULTIHITS && internedEntry.hits % 10 == 0)
					System.out.println("multihit "+internedEntry);
			}

		}
		
		// should not happen
		if(!toIntern.equals(internedString))
			System.err.println("mismatch");
		
		return internedString;
	}
	
	public static byte[] internBytes(byte[] toIntern) {
		
		if(toIntern == null)
			return null;
		
		byte[] internedArray;
		
		WeakByteArrayEntry checkEntry = new WeakByteArrayEntry(toIntern);

		synchronized( managedInterningSet){
			
			sanitize(false);
			
			WeakByteArrayEntry internedEntry = (WeakByteArrayEntry) managedInterningSet.get(checkEntry);
			
			if (internedEntry == null || (internedArray = internedEntry.getArray()) == null)
			{
				internedArray = toIntern;
				if(!managedInterningSet.add(checkEntry))
					System.out.println("unexpected modification");  // should not happen
			} else
			{
				internedEntry.incHits();
				checkEntry.destroy();
				if(TRACE_MULTIHITS && internedEntry.hits % 10 == 0)
					System.out.println("multihit"+internedEntry);
			}
		}
		
		// should not happen
		if(!Arrays.equals(toIntern, internedArray))
			System.err.println("mismatch");
		
		return internedArray;
	}
	
	/**
	 * A generic interning facilty for heavyweight or frequently duplicated
	 * Objects that have a reasonable <code>equals()</code> implementation.<br>
	 * <br>
	 * Important: The objects should have a limited lifespan, the interning set
	 * used by this method is unmanaged, i.e. does not clean out old entries!
	 * Entries without strong references are still removed.
	 * 
	 */
	public static Object internObject(Object toIntern)
	{
		if(toIntern == null)
			return null;
		
		Object internedItem;
		
		WeakEntry checkEntry = new WeakEntry(toIntern,unmanagedRefQueue);

		synchronized( unmanagedInterningSet ){
			
			WeakEntry internedEntry = (WeakEntry) unmanagedInterningSet.get(checkEntry);
			
			if (internedEntry == null || (internedItem = (internedEntry.get())) == null)
			{
				internedItem = toIntern;
				if(!unmanagedInterningSet.add(checkEntry))
					System.out.println("unexpected modification"); // should not happen
			}
			
			sanitizeLight();
		}
		
		// should not happen
		if(!toIntern.equals(internedItem))
			System.err.println("mismatch");
		
		return internedItem;
	}
	
	/**
	 * This is based on File.hashCode() and File.equals(), which can return different values for different representations of the same paths.
	 * Thus internFile should be used with canonized Files exclusively
	 */
	public static File internFile(File toIntern) {
		
		if(toIntern == null)
			return null;
		
		File internedFile;
		
		WeakFileEntry checkEntry = new WeakFileEntry(toIntern);

		synchronized( managedInterningSet ){
			
			sanitize(false);
					
			WeakFileEntry internedEntry = (WeakFileEntry) managedInterningSet.get(checkEntry);
			
			if (internedEntry == null || (internedFile = internedEntry.getFile()) == null)
			{
				internedFile = toIntern;
				if(!managedInterningSet.add(checkEntry))
					System.out.println("unexpected modification"); // should not happen
			} else
			{
				internedEntry.incHits();
				checkEntry.destroy();
				if(TRACE_MULTIHITS && internedEntry.hits % 10 == 0)
					System.out.println("multihit"+internedEntry);
			}
		}
		
		// should not happen
		if(!toIntern.equals(internedFile))
			System.err.println("mismatch");
		
		return internedFile;
	}
	
	public static URL internURL(URL toIntern) {
		
		if(toIntern == null)
			return null;
		
		URL internedURL;
		
		WeakURLEntry checkEntry = new WeakURLEntry(toIntern);

		synchronized( managedInterningSet ){
			
			sanitize(false);
					
			WeakURLEntry internedEntry = (WeakURLEntry) managedInterningSet.get(checkEntry);
			
			if (internedEntry == null || (internedURL = internedEntry.getURL()) == null)
			{
				internedURL = toIntern;
				if(!managedInterningSet.add(checkEntry))
					System.out.println("unexpected modification"); // should not happen
			} else
			{
				internedEntry.incHits();
				checkEntry.destroy();
				if(TRACE_MULTIHITS && internedEntry.hits % 10 == 0)
					System.out.println("multihit"+internedEntry);
			}
		}
		
		// should not happen
		if(!toIntern.toExternalForm().equals(internedURL.toExternalForm()))
			System.err.println("mismatch");
		
		return internedURL;
	}
	
	
	private final static Comparator	savingsComp	= new Comparator()
												{
													public int compare(Object o1, Object o2) {
														WeakWeightedEntry w1 = (WeakWeightedEntry) o1;
														WeakWeightedEntry w2 = (WeakWeightedEntry) o2;
														return w1.hits * w1.size - w2.hits * w2.size;
													}
												};
	
	private static void sanitizeLight()
	{
		synchronized (unmanagedInterningSet)
		{
			WeakEntry ref;
			while((ref = (WeakEntry)(unmanagedRefQueue.poll())) != null)
				unmanagedInterningSet.remove(ref);
			
			unmanagedInterningSet.compactify(-1f);
		}
	}
												
	private static void sanitize(boolean scheduled)
	{
		synchronized( managedInterningSet ){
			
			WeakWeightedEntry ref;
			while((ref = (WeakWeightedEntry)(managedRefQueue.poll())) != null)
			{
				if(!ref.isDestroyed())
				{
					managedInterningSet.remove(ref);
					if(TRACE_CLEANUP && ref.hits > 30)
						System.out.println("queue remove:"+ref);
				} else
				{// should not happen
					System.err.println("double removal "+ref);					
				}
			}
				
			
			int currentSetSize = managedInterningSet.size();
			
			aging:
			{
				cleanup:
				{
					// unscheduled cleanup/aging only in case of emergency
					if (currentSetSize < IMMEDIATE_CLEANUP_TRIGGER && !scheduled)
						break aging;
					
					if (TRACE_CLEANUP)
						System.out.println("Doing cleanup " + currentSetSize);
					
					ArrayList remaining = new ArrayList();
					
					// remove objects that aren't shared by multiple holders first (interning is useless)
					for (Iterator it = managedInterningSet.iterator(); it.hasNext();)
					{
						if (managedInterningSet.size() < IMMEDIATE_CLEANUP_GOAL && !scheduled)
							break aging;
						WeakWeightedEntry entry = (WeakWeightedEntry) it.next();
						if (entry.hits == 0)
						{
							if (TRACE_CLEANUP)
								System.out.println("0-remove: " + entry);
							it.remove();
						} else
							remaining.add(entry);
					}
					
					currentSetSize = managedInterningSet.size();
					if (currentSetSize < SCHEDULED_CLEANUP_TRIGGER && scheduled)
						break cleanup;
					if (currentSetSize < IMMEDIATE_CLEANUP_GOAL && !scheduled)
						break aging;
					
					Collections.sort(remaining, savingsComp);
					// remove those objects that saved the least amount first
					weightedRemove: for (int i = 0; i < remaining.size(); i++)
					{
						currentSetSize = managedInterningSet.size();
						if (currentSetSize < SCHEDULED_CLEANUP_GOAL && scheduled)
							break weightedRemove;
						if (currentSetSize < IMMEDIATE_CLEANUP_GOAL && !scheduled)
							break aging;
						WeakWeightedEntry entry = (WeakWeightedEntry) remaining.get(i);
						if (TRACE_CLEANUP)
							System.out.println("weighted remove: " + entry);
						managedInterningSet.remove(entry);
					}
				}
			
			
				currentSetSize = managedInterningSet.size();
				if (currentSetSize < SCHEDULED_AGING_THRESHOLD && scheduled)
					break aging;
				if (currentSetSize < IMMEDIATE_CLEANUP_GOAL && !scheduled)
					break aging;
				for (Iterator it = managedInterningSet.iterator(); it.hasNext();)
					((WeakWeightedEntry) it.next()).decHits();
			}
			
			if(TRACE_CLEANUP && scheduled)
			{
				List weightTraceSorted = new ArrayList(managedInterningSet);
				Collections.sort(weightTraceSorted,savingsComp);
				System.out.println("Remaining elements after cleanup:");
				for(Iterator it = weightTraceSorted.iterator();it.hasNext();)
					System.out.println("\t"+it.next());
			}
			
			if(scheduled)
				managedInterningSet.compactify(-1f);
				
		}
	}

	private static class WeakEntry extends WeakReference {
		private final int	hash;

		protected WeakEntry(Object o, ReferenceQueue q, int hash)
		{
			super(o, q);
			this.hash = hash;
		}

		public WeakEntry(Object o, ReferenceQueue q)
		{
			super(o, q);
			this.hash = o.hashCode();
		}

		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj instanceof WeakEntry)
			{
				Object myObj = get();
				Object otherObj = ((WeakEntry) obj).get();
				return myObj == null ? false : myObj.equals(otherObj);
			}
			return false;
		}

		public final int hashCode() {
			return hash;
		}
	}
	
	private static abstract class WeakWeightedEntry extends WeakEntry {
		final short	size;
		short		hits;

		public WeakWeightedEntry(Object o, int hash, int size)
		{
			super(o, managedRefQueue,hash);
			this.size = (short) (size & 0x7FFF);
		}

		public void incHits() {
			if (hits < Short.MAX_VALUE)
				hits++;
		}

		public void decHits() {
			if (hits > 0)
				hits--;
		}

		public String toString() {
			return this.getClass().getName().replaceAll("^.*\\..\\w+$", "") + " h=" + (int) hits + ";s=" + (int) size;
		}

		public void destroy() {
			hits = -1;
		}

		public boolean isDestroyed() {
			return hits == -1;
		}
	}
	
	private static class WeakByteArrayEntry extends WeakWeightedEntry {
		public WeakByteArrayEntry(byte[] array)
		{
			// byte-array object
			super(array, HashCodeUtils.hashCode(array), array.length + 8);
		}

		/**
		 * override equals since byte arrays need Arrays.equals
		 */
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj instanceof WeakByteArrayEntry)
			{
				byte[] myArray = getArray();
				byte[] otherArray = ((WeakByteArrayEntry) obj).getArray();
				return myArray == null ? false : Arrays.equals(myArray, otherArray);
			}
			return false;
		}

		public byte[] getArray() {
			return (byte[]) get();
		}

		public String toString() {
			return super.toString() + " " + (getArray() == null ? "null" : new String(getArray()));
		}
	}
	
	private static class WeakStringEntry extends WeakWeightedEntry {
		public WeakStringEntry(String entry)
		{
			// string object with 2 fields, char-array object
			super(entry, entry.hashCode(), 16 + 8 + entry.length() * 2);
		}

		public String getString() {
			return (String) get();
		}

		public String toString() {
			return super.toString() + " " + getString();
		}
	}
	
	private static class WeakFileEntry extends WeakWeightedEntry {
		public WeakFileEntry(File entry)
		{
			// file object with 2 fields, string object with 2 fields, char-array object
			super(entry, entry.hashCode(), 16 + 16 + 8 + entry.getPath().length() * 2);
		}

		public File getFile() {
			return (File) get();
		}

		public String toString() {
			return super.toString() + " " + getFile();
		}
	}
	
	private static class WeakURLEntry extends WeakWeightedEntry {
		public WeakURLEntry(URL entry)
		{
			// url object with 12 fields, ~4 string objects with 2 fields, 1 shared char-array object
			// use URL.toExternalForm().hashCode since URL.hashCode tries to resolve hostnames :(
			super(entry, entry.toExternalForm().hashCode(), 13 * 8 + 4 * 16 + 8 + entry.toString().length() * 2);
		}

		public URL getURL() {
			return (URL) get();
		}
		
		/**
		 * override equals since byte arrays need Arrays.equals
		 */
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj instanceof WeakURLEntry)
			{
				URL my = getURL();
				URL other = ((WeakURLEntry) obj).getURL();
				// use string compare as URL.equals tries to resolve hostnames
				return my == null ? false : my.toExternalForm().equals(other.toExternalForm());
			}
			return false;
		}

		public String toString() {
			return super.toString() + " " + getURL();
		}
	}
}
