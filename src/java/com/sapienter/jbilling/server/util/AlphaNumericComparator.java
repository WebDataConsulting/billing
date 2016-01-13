/*
jBilling - The Enterprise Open Source Billing System
Copyright (C) 2003-2011 Enterprise jBilling Software Ltd. and Emiliano Conde

This file is part of jbilling.

jbilling is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

jbilling is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License
along with jbilling.  If not, see <http://www.gnu.org/licenses/>.

 This source was modified by Web Data Technologies LLP (www.webdatatechnologies.in) since 15 Nov 2015.
You may download the latest source from webdataconsulting.github.io.

*/

package com.sapienter.jbilling.server.util;
import java.util.Comparator;
/**
 * This java comparator was added to address issue #7636 -
 * "Consumption attribute do not add in correct order when exceeds attribute 9" 
 * @author jbilling
 *
 */
public class AlphaNumericComparator implements Comparator <String> {
	 private final boolean isDigit(char ch)
	    {
	        return ch >= 48 && ch <= 57;
	    }

	    /** Length of string is passed in for improved efficiency (only need to calculate it once) **/
	    private final String getChunk(String s, int slength, int marker)
	    {
	        StringBuilder chunk = new StringBuilder();
	        char c = s.charAt(marker);
	        chunk.append(c);
	        marker++;
	        if (isDigit(c))
	        {
	            while (marker < slength)
	            {
	                c = s.charAt(marker);
	                if (!isDigit(c))
	                    break;
	                chunk.append(c);
	                marker++;
	            }
	        } else
	        {
	            while (marker < slength)
	            {
	                c = s.charAt(marker);
	                if (isDigit(c))
	                    break;
	                chunk.append(c);
	                marker++;
	            }
	        }
	        return chunk.toString();
	    }

		@Override
		public int compare(String o1, String o2) {
			 if (!(o1 != null) || !(o2 != null))
		        {
		            return 0;
		        }
		        String s1 = o1;
		        String s2 = o2;

		        int thisMarker = 0;
		        int thatMarker = 0;
		        int s1Length = s1.length();
		        int s2Length = s2.length();

		        while (thisMarker < s1Length && thatMarker < s2Length)
		        {
		            String thisChunk = getChunk(s1, s1Length, thisMarker);
		            thisMarker += thisChunk.length();

		            String thatChunk = getChunk(s2, s2Length, thatMarker);
		            thatMarker += thatChunk.length();

		            // If both chunks contain numeric characters, sort them numerically
		            int result = 0;
		            if (isDigit(thisChunk.charAt(0)) && isDigit(thatChunk.charAt(0)))
		            {
		                // Simple chunk comparison by length.
		                int thisChunkLength = thisChunk.length();
		                result = thisChunkLength - thatChunk.length();
		                // If equal, the first different number counts
		                if (result == 0)
		                {
		                    for (int i = 0; i < thisChunkLength; i++)
		                    {
		                        result = thisChunk.charAt(i) - thatChunk.charAt(i);
		                        if (result != 0)
		                        {
		                            return result;
		                        }
		                    }
		                }
		            } else
		            {
		                result = thisChunk.compareTo(thatChunk);
		            }

		            if (result != 0)
		                return result;
		        }

		        return s1Length - s2Length;
		}
}