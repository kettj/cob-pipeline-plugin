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

import java.io.IOException;
import java.util.List;

import org.kohsuke.stapler.QueryParameter;

import org.eclipse.egit.github.core.RepositoryId;
import org.eclipse.egit.github.core.RepositoryBranch;
import org.eclipse.egit.github.core.Team;
import org.eclipse.egit.github.core.client.GitHubClient;
import org.eclipse.egit.github.core.service.*;


/**
 * {@link Descriptor} for {@link RepositoryProperty}.
 * 
 * @author Jannik Kett
 */
public abstract class RepositoryPropertyDescriptor extends Descriptor<RepositoryProperty> {
    protected RepositoryPropertyDescriptor(Class<? extends RepositoryProperty> clazz) {
        super(clazz);
    }

    /**
     * Infers the type of the corresponding {@link Describable} from the outer class.
     * This version works when you follow the common convention, where a descriptor
     * is written as the static nested class of the describable class.
     *
     * @since 1.278
     */
    protected RepositoryPropertyDescriptor() {
    }

    /**
     * Whether or not the described property is enabled in the current context.
     * Defaults to true.  Over-ride in sub-classes as required.
     *
     * <p>
     * Returning false from this method essentially has the same effect of
     * making Hudson behaves as if this {@link RepositoryPropertyDescriptor} is
     * not a part of {@link RepositoryProperty#all()}.
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
    
    //TODO doCheckUrl
    
    //TODO 
    public ComboBoxModel doFillNameItems() {
    	ComboBoxModel items =new ComboBoxModel();
    	//ListBoxModel items = new ListBoxModel();    	
    	String githubLogin = Hudson.getInstance().getDescriptorByType(CobPipelineProperty.DescriptorImpl.class).getGithubLogin();
    	String githubPassword = Hudson.getInstance().getDescriptorByType(CobPipelineProperty.DescriptorImpl.class).getGithubPassword();
    	String githubOrg = Hudson.getInstance().getDescriptorByType(CobPipelineProperty.DescriptorImpl.class).getGithubOrg();
    	String githubTeam = Hudson.getInstance().getDescriptorByType(CobPipelineProperty.DescriptorImpl.class).getGithubTeam();
    	
    	try {
			GitHubClient client = new GitHubClient();
    		client.setCredentials(githubLogin, githubPassword);
    		TeamService githubTeamSrv = new TeamService(client);
    		Team team = new Team();
    		List<Team> teams = githubTeamSrv.getTeams(githubOrg);
    		for (Team t : teams) {
    			if (githubTeam.equals(t.getName())) {
    				team = t;
    			}
    		}    	
    		try {
    			List<org.eclipse.egit.github.core.Repository> teamRepos = githubTeamSrv.getRepositories(team.getId());
    			for (org.eclipse.egit.github.core.Repository repo : teamRepos) {
    				if (!items.contains(repo.getName()))
    					items.add(0, repo.getName());
    					//items.add(repo.getName(), repo.generateId());
    					//TODO add only if not already included
    			}
    		} catch (IOException ex) {
    			// TODO: handle exception
    		}
		} catch (IOException ex) {
			// TODO: handle exception
		}  	
		
    	return items;
    }
    
    public ComboBoxModel doFillForkItems(@QueryParameter String name) {
    	ComboBoxModel items = new ComboBoxModel();
    	if (name.length() == 0) {
    		items.add("Choose repository first");
    		return items;
    	}
    	String githubLogin = Hudson.getInstance().getDescriptorByType(CobPipelineProperty.DescriptorImpl.class).getGithubLogin();
    	String githubPassword = Hudson.getInstance().getDescriptorByType(CobPipelineProperty.DescriptorImpl.class).getGithubPassword();
    	String githubOrg = Hudson.getInstance().getDescriptorByType(CobPipelineProperty.DescriptorImpl.class).getGithubOrg();
    	
    	try {
			GitHubClient client = new GitHubClient();
    		client.setCredentials(githubLogin, githubPassword);
    		RepositoryService githubRepoSrv = new RepositoryService(client);
    		RepositoryId repoId = new RepositoryId(githubOrg, name);
    		List<org.eclipse.egit.github.core.Repository> forks = githubRepoSrv.getForks(repoId);
    		
    		for (org.eclipse.egit.github.core.Repository fork : forks) {
    			org.eclipse.egit.github.core.User user = fork.getOwner();
    			items.add(0, user.getLogin());
    		}
    		items.add(0, githubOrg);
    		
    	} catch (Exception ex) {
			// TODO: handle exception
		}      	
    	return items;
    }
    
    
    public ComboBoxModel doFillBranchItems(@QueryParameter String name, @QueryParameter String fork) {
    	ComboBoxModel items = new ComboBoxModel();
    	if (name.length() == 0 || fork.length() == 0) {
    		items.add("Choose repository and fork first");
    		return items;
    	}
    	String githubLogin = Hudson.getInstance().getDescriptorByType(CobPipelineProperty.DescriptorImpl.class).getGithubLogin();
    	String githubPassword = Hudson.getInstance().getDescriptorByType(CobPipelineProperty.DescriptorImpl.class).getGithubPassword();
    	
    	try {
    		GitHubClient client = new GitHubClient();
    		client.setCredentials(githubLogin, githubPassword);
    		RepositoryId repoId = new RepositoryId(fork, name);
    		RepositoryService githubRepoSrv = new RepositoryService(client);
    		List<RepositoryBranch> branches = githubRepoSrv.getBranches(repoId);
    		
    		for (RepositoryBranch branch : branches) {
    			items.add(branch.getName());
    		}   		
    		
		} catch (Exception e) {
			// TODO: handle exception
		}
    	
    	return items;
    }
}
