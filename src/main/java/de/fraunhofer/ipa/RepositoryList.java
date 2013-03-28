/**
 * Copyright (c) 2012
 * Fraunhofer Institute for Manufacturing Engineering
 * and Automation (IPA)
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * - Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 * - Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the
 * distribution.
 * - Neither the name of the Fraunhofer Institute for Manufacturing
 * Engineering and Automation (IPA) nor the names of its
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 *
 * This program is free software: you can redistribute it and/or
 * modify
 * it under the terms of the GNU Lesser General Public License LGPL as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License LGPL for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License LGPL along with this program.
 * If not, see <http://www.gnu.org/licenses/>.
 */

package de.fraunhofer.ipa;

import hudson.util.RobustCollectionConverter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import com.thoughtworks.xstream.XStream;


public class RepositoryList extends ArrayList<Repository>{
	public RepositoryList() {
	}

    public RepositoryList(Collection<? extends Repository> c) {
        super(c);
    }

    public RepositoryList(Repository... c) {
        this(Arrays.asList(c));
    }

    public Repository find(String name) {
        for (Repository r : this) {
            if(r.name.equals(name))
                return r;
        }
        return null;
    }
    
    @Override
    public boolean add(Repository repository) {
        return repository!=null && super.add(repository);
    }

	/**
	 * {@link Converter} implementation for XStream.
	 */
	public static final class ConverterImpl extends RobustCollectionConverter {
		public ConverterImpl(XStream xs) {
			super(xs);
		}

		@Override
		public boolean canConvert(Class type) {
			return type==RepositoryList.class;
		}

		@Override
		protected Object createCollection(Class type) {
			return new RepositoryList();
		}
	}
}
