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
import hudson.model.RootAction;
import hudson.model.User;
import hudson.model.UserProperty;
import hudson.model.UserPropertyDescriptor;
import hudson.tasks.MailAddressResolver;
import hudson.tasks.Mailer;
import hudson.util.FormValidation;

import java.io.IOException;
import java.util.List;
import java.util.ArrayList;

import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.servlet.ServletException;

import jenkins.model.Jenkins;
import net.sf.json.JSONObject;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.QueryParameter;

import org.eclipse.egit.github.core.*;
import org.eclipse.egit.github.core.client.GitHubClient;
import org.eclipse.egit.github.core.service.*;

/**
 * A UserProperty that can store a build pipeline
 * 
 * @author Jannik Kett
 */
public class CobPipelineProperty extends UserProperty {
	
	/**
	 * user email address
	 */
	private String email = null;
	
	/**
	 * user name
	 */
	private String userName = null;
	
	/**
	 * Jenkins master name
	 */
	private String masterName = null;
	
	/**
	 * Build repositories
	 */
	private volatile RootRepositoryList rootRepos = new RootRepositoryList();
	
	@DataBoundConstructor
	public CobPipelineProperty(String id) {
		/*TODO check if LDAPSecurityRealm is used
		 * 	   	check if MailAddressResolver is enabled
		 * 		try to get email from there
		 * try { 
			this.email = 
		} catch (Exception e) {
			// TODO: handle exception
		}*/
		this.userName = id;
		this.email = email;
		if (masterName == null) {
			this.masterName = getMasterName();
		}
		this.rootRepos = rootRepos;
	}
	
	public String defaultEmail() {
		if (email != null) {
			return email;
		} else {
			return "enter your email address here";
		}
		//TODO 
		/*else if (MailAddressResolver.resolve(User) == null) {
			return "enter your email address here";
		} else {
			return MailAddressResolver.resolve(User);
		}*/
	}
	
	public void setEmail(String email) {
		this.email = email;
	}
	
	private String getMasterName() {
		String url = Jenkins.getInstance().getRootUrl();
		if (url.endsWith("/")) {
			url = url.replace(":8080/", "");
		} else {
			url = url.replace(":8080", "");
		}
		url = url.replace("http://", "");
		
		return url;
	}
	
	public void setRootRepos(RootRepositoryList rootRepos) throws IOException {
		this.rootRepos = new RootRepositoryList(rootRepos);
		save();
	}
	
	public RootRepositoryList getRootRepos() {
		return rootRepos;
	}
	
    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl)super.getDescriptor();
    }
	
	@Extension
    public static class DescriptorImpl extends UserPropertyDescriptor {

		private String githubOrg;
		
		private String githubTeam;
		
		private String githubLogin;
		
		private String githubPassword;
		
		public DescriptorImpl() {
			load();
		}
		
        @Override
        public String getDisplayName() {
            return "Pipeline Configurations";
        }

        @Override
        public UserProperty newInstance(User user) {
            return new CobPipelineProperty(user.getId());
        }
        
        /**
         * Checks if the given String is a email address
         */
        public FormValidation doCheckEmail(@QueryParameter String value)
        		throws IOException, ServletException {
        	if (value.equals("enter your email address here")) {
        		return FormValidation.warning("email address not set yet");
        	}
    		try {
    			InternetAddress emailAddr = new InternetAddress(value);
    			emailAddr.validate();
    			return FormValidation.ok();
    		} catch (AddressException ex) {
    			return FormValidation.error("invalid email address");
    		}
    	}
        
        public void setGithubOrg(String githubOrg) {
        	this.githubOrg = githubOrg;
        }
        
        public String getGithubOrg() {
        	return githubOrg;
        }
        
        public void setGithubTeam(String githubTeam) {
        	this.githubTeam = githubTeam;
        }
        
        public String getGithubTeam() {
        	return githubTeam;
        }
        
        public void setGithubLogin(String githubLogin) {
        	this.githubLogin = githubLogin;
        }
        
        public String getGithubLogin() {
        	return githubLogin;
        }
        
        //TODO save password encrypted
        public void setGithubPassword(String githubPassword) {
        	this.githubPassword = githubPassword;
        }
        
        public String getGithubPassword() {
        	return githubPassword;
        }
        
        //TODO enhance output and order
        /**
         * Checks if organization exists
         */
        public FormValidation doCheckGithubOrg(@QueryParameter String value)
        		throws IOException, ServletException {
        	if (value.length() == 0) {
        		return FormValidation.error("Please enter organization name");
        	}
        	try {
        		OrganizationService githubOrgSrv = new OrganizationService();
        		org.eclipse.egit.github.core.User org = githubOrgSrv.getOrganization(value);
        		return FormValidation.ok("Organization ownes "+
                		org.getPublicRepos()+" public and "+org.getTotalPrivateRepos()+" private repositories");
        	} catch (IOException ex) {
        		return FormValidation.error("Invalid Github organization. Organization does not exist.\n"+ex.getMessage());
        	}
        }
              
        /**
         * Checks if given String is valid GitHub user
         */
        public FormValidation doCheckGithubLogin(@QueryParameter String value)
        		throws IOException, ServletException {
        	if (value.length() == 0) {
        		return FormValidation.error("Please enter login name");
        	} 
        	try {
        		UserService githubUserSrv = new UserService();
        		githubUserSrv.getUser(value);
        		return FormValidation.ok();
        	} catch (IOException ex) {
        		return FormValidation.error("Invalid Github user login. User does not exist.\n"+ex.getMessage());
        	}
        }
        
        /**
         * Checks if given password String is fits to GitHub user
         */
        public FormValidation doCheckGithubPassword(@QueryParameter String value, @QueryParameter String githubLogin)
        		throws IOException, ServletException {
        	if (value.length() == 0) {
        		return FormValidation.error("Please enter password");
        	}
        	try {
				GitHubClient client = new GitHubClient();
				client.setCredentials(githubLogin, value);
				UserService githubUserSrv = new UserService(client);
				org.eclipse.egit.github.core.User user = githubUserSrv.getUser(githubLogin);
				return FormValidation.ok("GitHub user name: "+user.getName()+"\nUser ownes "+
        		user.getPublicRepos()+" public and "+user.getTotalPrivateRepos()+" private repositories");
			} catch (Exception ex) {
				return FormValidation.error("Incorrect Password\n"+ex.getMessage());
			}
        }
        
        /**
         * Checks if team exists in given organization
         * and given user belongs to team
         */
        public FormValidation doCheckGithubTeam(@QueryParameter String value, @QueryParameter String githubLogin,
        		@QueryParameter String githubPassword, @QueryParameter String githubOrg)
        		throws IOException, ServletException {
        	if (value.length() == 0) {
        		return FormValidation.error("Please enter team name");
        	}
        	try {
        		GitHubClient client = new GitHubClient();
        		client.setCredentials(githubLogin, githubPassword);
        		TeamService githubTeamSrv = new TeamService(client);
        		List<Team> teams = githubTeamSrv.getTeams(githubOrg);
        		for (Team team : teams) {
        			if (value.equals(team.getName())) {
        				return FormValidation.ok("Team ownes "+Integer.toString(team.getReposCount())+" repositories.");
        			}
        		}
        		return FormValidation.error("Invalid Github team. Team not found. Make sure the given user is a member of this team");
        	} catch (IOException ex) {
        		return FormValidation.error("Error occured while checking team existenz: "+ex.getMessage());
        	}
        }
        
        @Override
        public boolean configure(StaplerRequest req, JSONObject form) throws FormException {
        	req.bindJSON(this, form);
        	super.save();
        	return super.configure(req, form);
        }
        
        /**
         * All {@link RepositoryDescriptor}s
         */
        public List<RepositoryPropertyDescriptor> getRootRepositoryDescriptors() {
        	List<RepositoryPropertyDescriptor> r = new ArrayList<RepositoryPropertyDescriptor>();
        	for (RepositoryPropertyDescriptor d : RootRepositoryProperty.all()) {
        		//add only RootRepositoryDescriptors
        		if (d.isRoot())
        			r.add(d);
        	}
        	return r;
        }
    }
    
    @Override
    public UserProperty reconfigure(StaplerRequest req, JSONObject form) throws FormException {
    	req.bindJSON(this, form);
    	return this;
    }
	
	
	public void save() throws IOException {
        user.save();
    }
	
	@Extension
	public static class GlobalAction implements RootAction {

		public String getDisplayName() {
			return "Pipeline Configuration";
		}
		
		public String getIconFileName() {
			// do not show when not logged in
			if (User.current() == null ) {
				return null;
			}
			return "/plugin/cob-pipeline/images/pipe_conf_small.png";
		}
		
		public String getUrlName() {
			if (User.current() == null ) {
				return null;
			}
			//return "/me/configure";
			return "/user/"+User.current().getId()+"/configure";
		}    
	}
}
