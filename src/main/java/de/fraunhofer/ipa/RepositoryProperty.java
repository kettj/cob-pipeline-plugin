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

import hudson.Extension;
import hudson.model.Descriptor.FormException;
import hudson.model.ReconfigurableDescribable;

import jenkins.model.Jenkins;
import net.sf.json.JSONObject;

import org.kohsuke.stapler.StaplerRequest;

public class RepositoryProperty implements ReconfigurableDescribable<RepositoryProperty> {
	
	/*
	 * name of repository
	 */
	private String name;
	
	/*
	 * repository name suffix
	 */
	private String suffix;
	
	/*
	 * full repository name
	 * name + suffix
	 */
	private String fullName;
	
	/*
	 * url address to repository
	 */
	private String url;
	
	
	@Override
    public  RepositoryPropertyDescriptor getDescriptor() {
		return (RepositoryPropertyDescriptor) Jenkins.getInstance().getDescriptorOrDie(getClass());
    }
	
	@Extension
    public static class DescriptorImpl extends RepositoryPropertyDescriptor {
		@Override
        public String getDisplayName() {
            return "Repository Configurations";
        }
		
		@Override
        public RepositoryProperty newInstance(Repository repository) {
            return new RepositoryProperty();
        }
	}
	
    @Override
    public RepositoryProperty reconfigure(StaplerRequest req, JSONObject form) throws FormException {
    	req.bindJSON(this, form);
    	return this;
    }

}
