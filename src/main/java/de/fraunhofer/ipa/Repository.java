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

import java.io.IOException;

import hudson.DescriptorExtensionList;
import hudson.Extension;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor.FormException;
import hudson.model.Hudson;
import hudson.util.ComboBoxModel;
import hudson.util.FormValidation;

import jenkins.model.Jenkins;
import net.sf.json.JSONObject;

import javax.servlet.ServletException;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

public class Repository extends AbstractDescribableImpl<Repository> implements Comparable<Repository> {
	
	/**
	 * name of repository
	 */
	protected String name;
	
	/**
	 * name of fork owner
	 */
	protected String fork = null;
	
	/**
	 * name of branch
	 */
	protected String branch = null;
	
	/**
	 * whether the repository should be polled be SCM
	 */
	protected Boolean poll;
	
	/**
	 * url address to repository
	 */
	protected String url;
	
	/**
	 * type of VCS, e.g. git, svn, hp, cvs
	 */
	protected String type;
	
	@DataBoundConstructor
	public Repository(String depName, String fork, String branch, Boolean poll) throws Exception {
		this.name = depName;
		if (fork.length() == 0) {
			this.fork = Hudson.getInstance().getDescriptorByType(CobPipelineProperty.DescriptorImpl.class).getDefaultFork();
		} else {
			this.fork = fork;
		}
		if (branch.length() == 0) {
			this.branch = Hudson.getInstance().getDescriptorByType(CobPipelineProperty.DescriptorImpl.class).getDefaultBranch();
		} else {
			this.branch = branch;
		}
		this.type = "git"; // right now only supported VCS is Git
		if (this.type.equals("git")) {
			this.url = "git@github.com:"+fork+"/"+depName+".git";
		} else {
			throw new Exception("Given VCS type '"+type+"' is not supported");
		}
		this.poll = poll;
	}
	
	public void setDepName(String depName){
		this.name = depName;
	}
	
	public String getDepName() {
		return this.name;
	}
	
	public void setFork(String fork){
		this.fork = fork;
	}
	
	public String getFork() {
		return this.fork;
	}
		
	public void setBranch(String branch){
		this.branch = branch;
	}
	
	public String getBranch() {
		return this.branch;
	}
	
	public void setPoll(Boolean poll) {
		this.poll = poll;
	}
	
	public Boolean getPoll() {
		return this.poll;
	}
		
	public void setUrl(String url) {
		//TODO check if url is valid
		// necessary? master url
		this.url = url;
	}
		
	@Override
    public RepositoryDescriptor getDescriptor() {
		return (RepositoryDescriptor)super.getDescriptor();
    }
	
	@Extension
    public static class DescriptorImpl extends RepositoryDescriptor {
		@Override
        public String getDisplayName() {
            return "Dependency Configurations"; //TODO
        }
	    
	    /**
	     * Fills combobox with repository names of organization
	     */
	    public ComboBoxModel doFillDepNameItems() {
	    	return super.doFillNameItems();
	    }
	    
	    /**
	     * Checks if given repository exists
	     */
	    public FormValidation doCheckDepName(@QueryParameter String value)
	    		throws IOException, ServletException {
	    	return super.doCheckName(value);
	    }
	    
	    /**
	     * Fill combobox with forks of repository
	     */
	    public ComboBoxModel doFillForkItems(@QueryParameter String depName) {
	    	return super.doFillForkItems(depName);
	    }
	    
	    /**
	     * Checks if given fork owner exists
	     */
	    public FormValidation doCheckFork(@QueryParameter String value, @QueryParameter String depName)
	    		throws IOException, ServletException {
	    	return super.doCheckFork(value, depName);
	    }

	    /**
	     * Fill combobox with branches of fork
	     */
	    public ComboBoxModel doFillBranchItems(@QueryParameter String depName, @QueryParameter String fork) {
	    	return super.doFillBranchItems(depName, fork);
	    }
	    
	    /**
	     * Checks if given branch exists
	     */
	    public FormValidation doCheckBranch(@QueryParameter String value, @QueryParameter String depName, @QueryParameter String fork)
	    		throws IOException, ServletException {
	    	return super.doCheckBranch(value, depName, fork);
	    }
	}
	
    public Repository reconfigure(StaplerRequest req, JSONObject form) throws FormException {
    	req.bindJSON(this, form);
    	return this;
    }
    
    /**
    * Returns all the registered {@link RepositoryDescriptor}s.
    */
    public static DescriptorExtensionList<Repository, RepositoryDescriptor> all() {
        return Jenkins.getInstance().<Repository, RepositoryDescriptor>getDescriptorList(Repository.class);
    }
    
    public int	compareTo(Repository that) {
		return this.name.compareTo(that.name);
	}
}
