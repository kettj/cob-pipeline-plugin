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
	
    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl)super.getDescriptor();
    }
	
	@Extension
    public static class DescriptorImpl extends UserPropertyDescriptor {

		private String githubAdmin;
		
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
        
        public void setGithubAdmin(String githubAdmin) {
        	this.githubAdmin = githubAdmin;
        }
        
        public String getGithubAdmin() {
        	return githubAdmin;
        }
        
        /**
         * Checks if given String is valid GitHub user
         */
        public FormValidation doCheckGithubAdmin(@QueryParameter String value)
        		throws IOException, ServletException {
        	if (value.length() == 0) {
        		return FormValidation.error("Please enter login name");
        	} 
        	try {
        		UserService githubUser = new UserService();
        		org.eclipse.egit.github.core.User user = githubUser.getUser(value);
        		return FormValidation.ok("GitHub user name: "+user.getName()+"\nUser ownes "+
        		user.getPublicRepos()+" public repositories");
        	} catch (IOException ex) {
        		return FormValidation.error("Invalid Github user login. User does not exist.");
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
        		//TODO only add RootRepositoryDescriptors
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
