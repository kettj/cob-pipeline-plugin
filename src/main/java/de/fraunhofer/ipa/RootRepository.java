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
import hudson.model.Hudson;
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
import org.kohsuke.stapler.bind.JavaScriptMethod;

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
	
	private Map<String, List<String>> matrixDistroArch;
	
	/**
	 * Jobs to include in pipeline
	 */
	private final ArrayList<String> jobs;
	
	/**
	 * Hardware/robots to build code and run tests on
	 */
	private final ArrayList<String> robots;
	
	/**
	 * Repository dependencies
	 */
	private final ArrayList<Repository> repoDeps;
	
	@DataBoundConstructor
	public RootRepository(String repoName, String fullName, String suffix, JSONObject rosDistro,
			String prioUbuntuDistro, String prioArch, String fork, String branch,
			JSONObject regularBuild, JSONObject downstreamBuild, JSONObject hardwareBuild,
			boolean release, List<Repository> repoDeps) throws Exception {
		super(repoName, fork, branch, true);
		if (suffix.length() == 0) {
			this.fullName = repoName;
		} else {
			this.fullName = this.name+"__"+suffix;
		}
		this.suffix = suffix;
		
		this.rosDistro = new ArrayList<String>();
		Iterator iter = rosDistro.keys();
		while(iter.hasNext()){
	        String key = (String)iter.next();
	        String value = rosDistro.getString(key);
	        if (value.equals("true")) {
	        	updateListItem(this.rosDistro, true, key);
	        }
		}
		
		this.prioUbuntuDistro = prioUbuntuDistro;
		this.prioArch = prioArch;
		
		this.matrixDistroArch = new HashMap<String, List<String>>();
		this.robots = new ArrayList<String>();
		this.jobs = new ArrayList<String>();
		updateList(jobs, regularBuild, "regular_build");
		updateList(jobs, downstreamBuild, "downstream_build");
		updateList(jobs, hardwareBuild, "hardware_build");
		updateListItem(jobs, release, "release");
		
		this.repoDeps = new ArrayList<Repository>(Util.fixNull(repoDeps));
	}
	
	public RootRepository(String repoName, String fullName, String suffix, JSONObject rosDistro,
			String prioUbuntuDistro, String prioArch, String fork, String branch,
			JSONObject regularBuild, JSONObject downstreamBuild, JSONObject hardwareBuild,
			boolean release, Repository... repoDeps) throws Exception {
		this(repoName, fullName, suffix, rosDistro,
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
		        	if (key.endsWith("__robot")) {
		        		this.robots.add(key.replace("__robot", ""));
		        	} else if (key.endsWith("__env")) {
		        		List<String> archs = new ArrayList<String>();
		        		String start = key.split("__")[0];
		        		if (parent.getString(start+"__amd64__env").equals("true")) {
		        			archs.add("amd64");
		        		}
		        		if (parent.getString(start+"__i386__env").equals("true")) {
		        			archs.add("i386");
		        		}
		        		this.matrixDistroArch.put(start, archs);
		        	} else {
		        		list.add(StringUtils.join(key.split("(?=\\p{Upper})"), "_").toLowerCase());
		        	}
		        }
			}
		}
	}
	
	private void updateListItem(List<String> list, boolean item, String value) {
		if (item) {
			list.add(value);
		} 
	}
		
	public void setRepoName(String repoName) {
		this.name = repoName;
	}
	
	public String getRepoName() {
		return this.name;
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
	
	public boolean isRosDistroChecked(String rosDistro) {
		return this.rosDistro.contains(rosDistro);
	}
	
	public List<String> getRosDistro() {
		return this.rosDistro;
	}
	
	public void setPrioUbuntuDistro(String prioUbuntuDistro) {
		this.prioUbuntuDistro = prioUbuntuDistro;
	}
	
	@JavaScriptMethod
	public String getPrioUbuntuDistro() {
		return this.prioUbuntuDistro;
	}
	
	public void setPrioArch(String prioArch) {
		this.prioArch = prioArch;
	}
	
	public String getPrioArch() {
		return this.prioArch;
	}
	
	@JavaScriptMethod
	public boolean isMatrixEntryChecked(String ubuntu, String arch) {
		if (this.matrixDistroArch.containsKey(ubuntu)) {
			if (this.matrixDistroArch.get(ubuntu).contains(arch)) {
				return true;
			}
		}
		return false;
	}
	
	public Map<String, List<String>> getMatrixDistroArch() {
		return this.matrixDistroArch;
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
	
	public List<String> getJobs() {
		return this.jobs;
	}
	
	public boolean isRobotChecked(String robot) {
		return this.robots.contains(robot);
	}
	
	public List<String> getRobots() {
		return this.robots;
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
            return "Repository Configurations";
        }
		
		@Override
		public boolean isRoot() {
			return true;
		}
	    
	    /**
	     * Returns list of global defined supported ROS releases
	     * @return
	     */
	    public List<String> getAllRosDistros() {
	    	return Hudson.getInstance().getDescriptorByType(CobPipelineProperty.DescriptorImpl.class).getAllRosDistros();
	    }
	    
	    public List<String> getUbuntuReleases() {
	    	List<String> ubuntuReleases = new ArrayList<String>();
	    	List<Map<String, List<String>>> targets = Hudson.getInstance().getDescriptorByType(CobPipelineProperty.DescriptorImpl.class).getTargets();
	    	
	    	for (Map<String, List<String>> ros : targets) {
	    		if (!ros.keySet().iterator().next().equals("backports")) {
		    		for (List<String> ubuntuList : ros.values()) {
		    			for (String ubuntu : ubuntuList) {
		    				if (!ubuntuReleases.contains(ubuntu)) {
		    					ubuntuReleases.add(ubuntu);
		    				}
		    			}
		    		}
	    		}
	    	}
	    	return ubuntuReleases;
	    }
	    
	    public String getSupportedROS(String ubuntuDistro) {
	    	List<String> rosDistros = new ArrayList<String>();	    	
	    	List<Map<String, List<String>>> targets = Hudson.getInstance().getDescriptorByType(CobPipelineProperty.DescriptorImpl.class).getTargets();
	    	
	    	for (Map<String, List<String>> ros : targets) {
	    		for (List<String> ubuntuList : ros.values()) {
	    			if (ubuntuList.contains(ubuntuDistro) && !ros.keySet().iterator().next().equals("backports")) {
	    				rosDistros.add(ros.keySet().iterator().next());
	    			}
	    		}
	    	}
	    		    	
	    	return rosDistros.toString();
	    }
	    
	    /**
	     * Returns list of global defined available robots
	     * @return
	     */
	    public List<String> getRobots() {
	    	return Hudson.getInstance().getDescriptorByType(CobPipelineProperty.DescriptorImpl.class).getRobots();
	    }
		
		/*public ListBoxModel doFillPrioUbuntuDistroItems() {
			ListBoxModel prioDistroItems = new ListBoxModel();
			
			List<String> ubuntuDistros = getUbuntuReleases();
			for (String ubuntuDistro : ubuntuDistros) {
				prioDistroItems.add(ubuntuDistro + " " + getSupportedROS(ubuntuDistro), ubuntuDistro);
			}
			
			return prioDistroItems;
		}*/
		
		public ListBoxModel doFillPrioArchItems() {
			ListBoxModel prioArchItems = new ListBoxModel();
			prioArchItems.add("amd64");
			prioArchItems.add("i386");
			
			return prioArchItems;
		}
		
		public String getDefaultFork() {
			return Hudson.getInstance().getDescriptorByType(CobPipelineProperty.DescriptorImpl.class).getDefaultFork();
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
    
    public int	compareTo(RootRepository that) {
		return this.fullName.compareTo(that.fullName);
	}
}
