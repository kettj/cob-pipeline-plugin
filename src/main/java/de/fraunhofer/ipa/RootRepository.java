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

import hudson.DescriptorExtensionList;
import hudson.Extension;
import hudson.Util;
import hudson.model.Descriptor.FormException;
import hudson.model.User;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;

import jenkins.model.Jenkins;
import net.sf.json.JSONObject;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;

import javax.servlet.ServletException;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.QueryParameter;

import org.apache.commons.lang.StringUtils;

public class RootRepository extends Repository {

	/**
	 * 
	 */
	private String suffix;

	/**
	 * 
	 */
	protected String fullName;
	
	/**
	 * List of ros distros to build
	 */
	private final ArrayList<String> rosDistro;
	
	/**
	 * Ubuntu distro to build with priority
	 */
	private String prioUbuntuDistro;
	
	/**
	 * Architecture to build with priority
	 */
	private String prioArch;
	
	/**
	 * Jobs to include in pipeline
	 */
	private final ArrayList<String> jobs;
	
	/**
	 * Repository dependencies
	 */
	private final ArrayList<Repository> repoDeps;
	
	@DataBoundConstructor
	public RootRepository(String repoName, String fullName, String suffix,
			boolean electric, boolean fuerte, boolean groovy, boolean hydro,
			String prioUbuntuDistro, String prioArch, String fork, String branch,
			JSONObject regularBuild, JSONObject downstreamBuild, JSONObject hardwareBuild,
			boolean release, List<Repository> repoDeps) {
		super(repoName, fork, branch, true);
		if (suffix.length() == 0) {
			this.fullName = repoName;
		} else {
			this.fullName = this.repoName+"__"+suffix;
		}
		this.suffix = suffix;
		
		this.rosDistro = new ArrayList<String>();
		updateListItem(this.rosDistro, electric, "electric");
		updateListItem(this.rosDistro, fuerte, "fuerte");
		updateListItem(this.rosDistro, groovy, "groovy");
		updateListItem(this.rosDistro, hydro, "hydro");
		
		this.prioUbuntuDistro = prioUbuntuDistro;
		this.prioArch = prioArch;
		
		this.jobs = new ArrayList<String>();
		updateListItem(jobs, (regularBuild != null), "regular_build");
		updateList(jobs, downstreamBuild, "downstream_build");
		updateList(jobs, hardwareBuild, "hardware_build");
		updateListItem(jobs, release, "release");
		
		this.repoDeps = new ArrayList<Repository>(Util.fixNull(repoDeps));
	}
	
	public RootRepository(String repoName, String fullName, String suffix,
			boolean electric, boolean fuerte, boolean groovy, boolean hydro,
			String prioUbuntuDistro, String prioArch, String fork, String branch,
			JSONObject regularBuild, JSONObject downstreamBuild, JSONObject hardwareBuild,
			boolean release, Repository... repoDeps) {
		this(repoName, fullName, suffix, electric, fuerte, groovy, hydro,
				prioUbuntuDistro, prioArch, fork, branch, regularBuild, downstreamBuild,
				hardwareBuild, release, Arrays.asList(repoDeps));
	}
	
	private void updateList(List<String> list, JSONObject parent, String name) {
		if (parent != null) {
			list.add(name);
			Iterator iter = parent.keys();
			while(iter.hasNext()){
		        String key = (String)iter.next();
		        String value = parent.getString(key);
		        if (value.equals("true")) {
		        	list.add(StringUtils.join(key.split("(?=\\p{Upper})"), "_").toLowerCase());
		        }
			}
		}
	}
	
	private void updateListItem(List<String> list, boolean item, String value) {
		if (item) {
			list.add(value);
		} 
	}
		
	@Override
	public void setRepoName(String repoName) {
		this.repoName = repoName;
	}
	
	public String getFullName() {
		return this.fullName;
	}
	
	public void setSuffix(String suffix) {
		this.suffix = suffix;
	}
	
	public String getSuffix() {
		return this.suffix;
	}
	
	public boolean getElectric() {
		return this.rosDistro.contains("electric");
	}
	
	public boolean getFuerte() {
		return this.rosDistro.contains("fuerte");
	}
	
	public boolean getGroovy() {
		return this.rosDistro.contains("groovy");
	}
	
	public boolean getHydro() {
		return this.rosDistro.contains("hydro");
	}
	
	public void setPrioUbuntuDistro(String prioUbuntuDistro) {
		this.prioUbuntuDistro = prioUbuntuDistro;
	}
	
	public String getPrioUbuntuDistro() {
		return this.prioUbuntuDistro;
	}
	
	public void setPrioArch(String prioArch) {
		this.prioArch = prioArch;
	}
	
	public String getPrioArch() {
		return this.prioArch;
	}
	
	public boolean getRegularBuild() {
		return this.jobs.contains("regular_build");
	}
	
	public boolean getDownstreamBuild() {
		return this.jobs.contains("downstream_build");
	}
	
	public boolean getNongraphicsTest(){
		return this.jobs.contains("nongraphics_test");
	}
	
	public boolean getGraphicsTest() {
		return this.jobs.contains("graphics_test");
	}
	
	public boolean getHardwareBuild() {
		return this.jobs.contains("hardware_build");
	}
	
	public boolean getAutomaticHwTest() {
		return this.jobs.contains("automatic_hw_test");
	}
	
	public boolean getInteractiveHwTest() {
		return this.jobs.contains("interactive_hw_test");
	}
	
	public boolean getRelease() {
		return this.jobs.contains("release");
	}
	
	public List<Repository> getRepoDeps() {
		return repoDeps;
	}
	
	@Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl)super.getDescriptor();
    }
	
	@Extension
    public static class DescriptorImpl extends RepositoryDescriptor {
		@Override
        public String getDisplayName() {
            return "Root Repository Configurations";
        }
		
		@Override
		public boolean isRoot() {
			return true;
		}
		
		public FormValidation doCheckSuffix(@QueryParameter String value, @QueryParameter String repoName)
				throws IOException, ServletException {
			//TODO check if other repo with the same name exists
			
			if (value.length() != 0) { 
				return FormValidation.ok("Full name: "+repoName+"__"+value);
			} else {
				return FormValidation.ok("Full name: "+repoName);
			}
		}
		
		public ListBoxModel doFillPrioUbuntuDistroItems() {
			ListBoxModel prioDistroItems = new ListBoxModel();
			
			//TODO get list of ros_distro
			//TODO get target platforms from github
			//TODO calc possible ubuntu distros
			prioDistroItems.add("test_distro1");
			prioDistroItems.add("test_distro2");
			
			return prioDistroItems;
		}
		
		public ListBoxModel doFillPrioArchItems() {
			ListBoxModel prioArchItems = new ListBoxModel();
			prioArchItems.add("amd64");
			prioArchItems.add("i386");
			
			return prioArchItems;
		}
        
        /**
         * All {@link RepositoryDescriptor}s
         */
        public List<RepositoryDescriptor> getRepositoryDescriptors() {
        	List<RepositoryDescriptor> r = new ArrayList<RepositoryDescriptor>();
        	for (RepositoryDescriptor d : Repository.all()) {
        		// add only RepositoryDescriptors not RootRepositoryDescriptors
        		if (!d.isRoot())
        			r.add(d);
        	}
        	return r;
        }
	}

	@Override
    public Repository reconfigure(StaplerRequest req, JSONObject form) throws FormException {
    	req.bindJSON(this, form);
    	return this;
    }
    
    /**
     * Returns all the registered {@link RepositoryDescriptor}s.
     */
    //return only Root
    public static DescriptorExtensionList<Repository, RepositoryDescriptor> all() {
        return Jenkins.getInstance().<Repository, RepositoryDescriptor>getDescriptorList(Repository.class);
    }
}
