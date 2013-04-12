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

import hudson.model.Descriptor;
import hudson.model.Hudson;
import hudson.util.ComboBoxModel;
import hudson.util.FormValidation;

import java.io.IOException;
import java.util.List;

import javax.mail.Message;
import javax.servlet.ServletException;

import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.bind.JavaScriptMethod;

import org.eclipse.egit.github.core.RepositoryId;
import org.eclipse.egit.github.core.RepositoryBranch;
import org.eclipse.egit.github.core.client.GitHubClient;
import org.eclipse.egit.github.core.service.*;


/**
 * {@link Descriptor} for {@link Repository}.
 * 
 * @author Jannik Kett
 */
public abstract class RepositoryDescriptor extends Descriptor<Repository> {
    protected RepositoryDescriptor(Class<? extends Repository> clazz) {
        super(clazz);
    }
    
    private String githubOrg;
    
    private GitHubClient githubClient = new GitHubClient();
    
    private ComboBoxModel repoNameItems = null;
    private ComboBoxModel forkItems = null;
    private ComboBoxModel branchItems = null;
    
    /**
     * Infers the type of the corresponding {@link Describable} from the outer class.
     * This version works when you follow the common convention, where a descriptor
     * is written as the static nested class of the describable class.
     *
     * @since 1.278
     */
    protected RepositoryDescriptor() {
    }

    /**
     * Whether or not the described property is enabled in the current context.
     * Defaults to true.  Over-ride in sub-classes as required.
     *
     * <p>
     * Returning false from this method essentially has the same effect of
     * making Hudson behaves as if this {@link RepositoryDescriptor} is
     * not a part of {@link Repository#all()}.
     *
     * <p>
     * This mechanism is useful if the availability of the property is
     * contingent of some other settings. 
     */
    public boolean isEnabled() {
        return true;
    }
    
    /**
    * Return false if the user shouldn't be able to create this repository from the UI.
    */
    public boolean isInstantiable() {
        return true;
    }
    
    /**
     * Return true if the repository is a root repository.
     */
    public boolean isRoot() {
    	return false;
    }
    
    /**
     * Sets the globally given GitHub configurations
     */
    private void setGithubConfig() {
    	String githubLogin = Hudson.getInstance().getDescriptorByType(CobPipelineProperty.DescriptorImpl.class).getGithubLogin();
    	String githubPassword = Hudson.getInstance().getDescriptorByType(CobPipelineProperty.DescriptorImpl.class).getGithubPassword();
    	
    	this.githubOrg = Hudson.getInstance().getDescriptorByType(CobPipelineProperty.DescriptorImpl.class).getGithubOrg();
    	
    	this.githubClient.setCredentials(githubLogin, githubPassword);
    }
    
    /**
     * Fills combobox with repository names of organization
     */
    public ComboBoxModel doFillNameItems() {
    	ComboBoxModel aux = new ComboBoxModel();

    	if (this.githubClient.getUser() == "") {
    		setGithubConfig();
    	}
    	
    	try {
    		RepositoryService githubRepoSrv = new RepositoryService(githubClient);
    		List<org.eclipse.egit.github.core.Repository> repos = githubRepoSrv.getRepositories(
    				Hudson.getInstance().getDescriptorByType(CobPipelineProperty.DescriptorImpl.class).getDefaultFork());
    		for (org.eclipse.egit.github.core.Repository repo : repos) {
    			if (!aux.contains(repo.getName()))
					aux.add(0, repo.getName());
    		}
		} catch (IOException ex) {
			// TODO: handle exception
		}  
    	
    	return this.repoNameItems = aux;
    }
    
    /**
     * Checks if given repository exists
     */
    public FormValidation doCheckName(@QueryParameter String value)
    		throws IOException, ServletException {
    	
    	doFillNameItems();
    	
    	if (value.length() == 0) {
    		return FormValidation.warning(Messages.Repository_NoName());
    	}
    	
    	// check if given repository is in repo list
    	for (String repoName : this.repoNameItems) {
			if (repoName.equals(value)) {
				return FormValidation.ok();
			}
		}
    	// if repository was not in list, for example extern repository
    	// TODO if owner is not given, ask for owner and check for repo    	
    	
    	return FormValidation.error(Messages.Repository_NoFound());
    }
    
    
    /**
     * Fill combobox with forks of repository
     */
    public ComboBoxModel doFillForkItems(@QueryParameter String name) {
    	ComboBoxModel aux = new ComboBoxModel();
    	
    	if (this.githubOrg == null) {
    		setGithubConfig();
    	}
    	
    	try {
    		RepositoryService githubRepoSrv = new RepositoryService(this.githubClient);
    		RepositoryId repoId = new RepositoryId(this.githubOrg, name);
    		List<org.eclipse.egit.github.core.Repository> forks = githubRepoSrv.getForks(repoId);
    		
    		for (org.eclipse.egit.github.core.Repository fork : forks) {
    			org.eclipse.egit.github.core.User user = fork.getOwner();
    			aux.add(0, user.getLogin());
    		}
    		aux.add(0, this.githubOrg);
    		
    	} catch (Exception ex) {
			// TODO: handle exception
		}      	
    	return this.forkItems = aux;
    }
    
    @JavaScriptMethod
    public String checkFork(String fork, String repo) {
    	String msg = "";
    	
    	doFillForkItems(repo);
    	
    	if (fork.length() == 0) {
    		fork = Hudson.getInstance().getDescriptorByType(CobPipelineProperty.DescriptorImpl.class).getDefaultFork();
    		msg = Messages.Fork_DefaultUsed(fork);
    	}
    	
    	// check if given fork owner is in fork list
    	for (String f : this.forkItems) {
			if (f.equals(fork)) {
				return msg;
			}
		}
    	
    	// if fork owner was not in list
    	try {
    		// check if user exists
			try {
				UserService githubUserSrv = new UserService(this.githubClient);
				githubUserSrv.getUser(fork);
			} catch (Exception ex) {
				return msg + Messages.Fork_OwnerNotFound() + "\n" + ex.getMessage();
			}
			// check if user has public repository with given name
			try {
				RepositoryService githubRepoSrv = new RepositoryService(this.githubClient);
				List<org.eclipse.egit.github.core.Repository> repos = githubRepoSrv.getRepositories(fork);
				for (org.eclipse.egit.github.core.Repository r : repos) {
					if (r.getName().equals(repo))
						return msg + Messages.Fork_Found();
				}
			} catch (Exception ex) {
				return msg + Messages.Fork_GetReposFailed() + "\n" + ex.getMessage();
			}
			return msg + Messages.Fork_NotFound(repo, fork);
		} catch (Exception ex) {
			return Messages.Fork_AuthFailed() + "\n" + ex.getMessage();
		}
    }
    
    /**
     * Checks if given fork owner exists
     */
    /*public FormValidation doCheckFork(@QueryParameter String value, @QueryParameter String name)
    		throws IOException, ServletException {
    	
    	String msg = "";
    	
    	doFillForkItems(name);
    	    	
    	if (value.length() == 0) {
    		value = Hudson.getInstance().getDescriptorByType(CobPipelineProperty.DescriptorImpl.class).getDefaultFork();
    		msg = Messages.Fork_DefaultUsed(value);
    	}
    	
    	// check if given fork owner is in fork list
    	for (String fork : this.forkItems) {
			if (fork.equals(value)) {
				return (msg.length()==0) ? FormValidation.ok() : FormValidation.ok(msg);
			}
		}
    	
    	// if fork owner was not in list
    	try {
    		// check if user exists
			try {
				UserService githubUserSrv = new UserService(this.githubClient);
				githubUserSrv.getUser(value);
			} catch (Exception ex) {
				return FormValidation.error(msg + Messages.Fork_OwnerNotFound() + "\n" + ex.getMessage());
			}
			// check if user has public repository with given name
			try {
				RepositoryService githubRepoSrv = new RepositoryService(this.githubClient);
				List<org.eclipse.egit.github.core.Repository> repos = githubRepoSrv.getRepositories(value);
				for (org.eclipse.egit.github.core.Repository repo : repos) {
					if (repo.getName().equals(name))
						return FormValidation.ok(msg + Messages.Fork_Found());
				}
			} catch (Exception ex) {
				return FormValidation.error(msg + Messages.Fork_GetReposFailed() + "\n" + ex.getMessage());
			}
			return FormValidation.error(msg + Messages.Fork_NotFound(value));
		} catch (Exception ex) {
			return FormValidation.error(Messages.Fork_AuthFailed() + "\n" + ex.getMessage());
		}
    }*/
        
    /**
     * Fills combobox with branches of given repository fork
     * @param repoName
     * @param fork
     * @return
     */
    public ComboBoxModel doFillBranchItems(@QueryParameter String name, @QueryParameter String fork) {
    	ComboBoxModel aux = new ComboBoxModel();
    	
    	if (this.githubOrg == null) {
    		setGithubConfig();
    	}
    	
    	if (fork.length() == 0) {
    		fork = Hudson.getInstance().getDescriptorByType(CobPipelineProperty.DescriptorImpl.class).getDefaultFork();
    	}
    	
    	try {
    		RepositoryId repoId = new RepositoryId(fork, name);
    		RepositoryService githubRepoSrv = new RepositoryService(this.githubClient);
    		List<RepositoryBranch> branches = githubRepoSrv.getBranches(repoId);
    		
    		for (RepositoryBranch branch : branches) {
    			aux.add(branch.getName());
    		}   		
    		
		} catch (Exception e) {
			// TODO: handle exception
		}

    	return this.branchItems = aux;
    }
    
    @JavaScriptMethod
    public String checkBranch(String branch, String fork, String repo) {
    	String msg = "";
    	
    	doFillBranchItems(repo, fork);
    	
    	if (branch.length() == 0) {
    		branch = Hudson.getInstance().getDescriptorByType(CobPipelineProperty.DescriptorImpl.class).getDefaultBranch();
    		msg = Messages.Branch_DefaultUsed(branch);
    	}
    	
    	// check if given branch is in branch list
    	for (String b : this.branchItems) {
			if (b.equals(branch)) {
				return msg;
			}
		}
    	    	
    	return msg + Messages.Branch_NotFound();
    }
    
    /**
     * Checks if given branch exists
     */
    /*public FormValidation doCheckBranch(@QueryParameter String value, @QueryParameter String name, @QueryParameter String fork)
    		throws IOException, ServletException {
    	
    	String msg = "";
    	
    	doFillBranchItems(name, fork);
    	
    	if (value.length() == 0) {
    		value = Hudson.getInstance().getDescriptorByType(CobPipelineProperty.DescriptorImpl.class).getDefaultBranch();
    		msg = Messages.Branch_DefaultUsed(value);
    	}
    	
    	// check if given branch is in branch list
    	for (String branch : this.branchItems) {
			if (branch.equals(value)) {
				return (msg.length()==0) ? FormValidation.ok() : FormValidation.ok(msg);
			}
		}
    	    	
    	return FormValidation.error(msg + Messages.Branch_NotFound());
    }*/
}
