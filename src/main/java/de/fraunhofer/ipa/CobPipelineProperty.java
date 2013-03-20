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

import hudson.BulkChange;
import hudson.Extension;
import hudson.Util;
import hudson.model.Descriptor;
import hudson.model.Descriptor.FormException;
import hudson.model.RootAction;
import hudson.model.User;
import hudson.model.listeners.SaveableListener;
import hudson.model.UserProperty;
import hudson.model.UserPropertyDescriptor;
import hudson.tasks.MailAddressResolver;
import hudson.tasks.Mailer;
import hudson.util.FormValidation;
import hudson.util.QuotedStringTokenizer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Writer;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

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
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.storage.file.FileRepository;

import org.yaml.snakeyaml.*;

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

	private String defaultFork;

	private String defaultBranch;

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

	public void setDefaultFork(String fork) {
		this.defaultFork = fork;
	}

	public String getDefaultFork() {
		return this.defaultFork;
	}

	public void setDefaultBranch(String branch) {
		this.defaultBranch = branch;
	}

	public String getDefaultBranch() {
		return this.defaultBranch;
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

		private ArrayList<String> allRosDistros;

		private ArrayList<String> robots;

		private String targetsURL;

		private List<Map<String, List<String>>> targets;

		private String defaultFork;

		private String defaultBranch;

		private String configRepoURL;

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

		public void setAllRosDistrosString(String rosDistrosString) {
			this.allRosDistros = new ArrayList<String>(Arrays.asList(Util.tokenize(rosDistrosString)));
		}

		public String getAllRosDistrosString() {
			int len=0;
			for (String rosDistro : allRosDistros)
				len += rosDistro.length();
			char delim = len>30 ? '\n' : ' ';
			// Build string connected with delimiter, quoting as needed
			StringBuilder buf = new StringBuilder(len+allRosDistros.size()*3);
			for (String rosDistro : allRosDistros)
				buf.append(delim).append(QuotedStringTokenizer.quote(rosDistro,""));
			return buf.substring(1);
		}

		public List<String> getAllRosDistros() {
			return Collections.unmodifiableList(allRosDistros);
		}

		public void setRobotsString(String robotsString) {
			this.robots = new ArrayList<String>(Arrays.asList(Util.tokenize(robotsString)));
		}

		public String getRobotsString() {
			int len=0;
			for (String robot : robots)
				len += robot.length();
			char delim = len>30 ? '\n' : ' ';
			// Build string connected with delimiter, quoting as needed
			StringBuilder buf = new StringBuilder(len+robots.size()*3);
			for (String robot : robots)
				buf.append(delim).append(QuotedStringTokenizer.quote(robot,""));
			return buf.substring(1);
		}

		public List<String> getRobots() {
			return Collections.unmodifiableList(robots);
		}

		public void setTargetsURL(String url) throws Exception{
			this.targetsURL = url;
			URL targets = new URL(url);
			BufferedReader in = new BufferedReader(
					new InputStreamReader(targets.openStream()));

			String aux = "";
			String yamlString = "";
			while ((aux = in.readLine()) != null) {
				yamlString += aux;
				yamlString += "\n";
			}
			in.close();

			Yaml yaml = new Yaml();
			this.targets = new ArrayList<Map<String, List<String>>>();
			this.targets = (List<Map<String, List<String>>>) yaml.load(yamlString);
		}

		public String getTargetsURL() {
			return this.targetsURL;
		}

		public List<Map<String, List<String>>> getTargets() {
			return this.targets;
		}

		public void setConfigRepoURL(String configRepoURL) {
			this.configRepoURL = configRepoURL;
		}

		public String getConfigRepoURL() {
			return this.configRepoURL;
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

		public FormValidation doCheckTargets(@QueryParameter String value)
				throws IOException, ServletException {
			if (value.length() == 0) {
				return FormValidation.warning("Please enter URL of the target platform yaml file");
			}
			return FormValidation.ok();
		}

		public FormValidation doCheckDefaultFork(@QueryParameter String value)
				throws IOException, ServletException {
			if (value.length() == 0) {
				return FormValidation.warning("Please enter default fork/owner.");
			}
			this.defaultFork = value;
			// TODO check if user exists
			return FormValidation.ok();
		}

		public String getDefaultFork() {
			return this.defaultFork;
		}

		public FormValidation doCheckDefaultBranch(@QueryParameter String value)
				throws IOException, ServletException {
			if (value.length() == 0) {
				return FormValidation.warning("Please enter default branch.");
			}
			this.defaultBranch = value;
			// TODO check if user exists
			return FormValidation.ok();
		}

		public String getDefaultBranch() {
			return this.defaultBranch;
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
		public List<RepositoryDescriptor> getRootRepositoryDescriptors() {
			List<RepositoryDescriptor> r = new ArrayList<RepositoryDescriptor>();
			for (RepositoryDescriptor d : RootRepository.all()) {
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
		try {
			Map<String, Object> data = new HashMap<String, Object>();
			data.put("user_name", this.userName);
			data.put("server_name", this.masterName);
			data.put("email", this.email);
			data.put("committer_email_enabled", true);
			Map<String, Object> repos = new HashMap<String, Object>();
			for (RootRepository rootRepo : this.rootRepos) {
				Map<String, Object> repo = new HashMap<String, Object>();
				repo.put("type", rootRepo.type);
				repo.put("url", rootRepo.url);
				repo.put("version", rootRepo.branch);
				repo.put("poll", rootRepo.poll);
				repo.put("ros_distro", rootRepo.getRosDistro());
				repo.put("prio_ubuntu_distro", rootRepo.getPrioUbuntuDistro());
				repo.put("prio_arch", rootRepo.getPrioArch());
				repo.put("matrix_distro_arch", rootRepo.getMatrixDistroArch());
				repo.put("jobs", rootRepo.getJobs());
				repo.put("robots", rootRepo.getRobots());

				Map<String, Object> deps = new HashMap<String, Object>();
				for (Repository repoDep : rootRepo.getRepoDeps()) {
					Map<String, Object> dep = new HashMap<String, Object>();
					dep.put("type", repoDep.type);
					dep.put("url", repoDep.url);
					dep.put("version", repoDep.branch);
					dep.put("poll", repoDep.poll);
					deps.put(repoDep.repoName, dep);
				}
				repo.put("dependencies", deps);

				repos.put(rootRepo.fullName, repo);
			}
			data.put("repositories", repos);
			Yaml yaml = new Yaml();
			yaml.dump(data, getPipelineConfigFile());
			LOGGER.log(Level.INFO, "Created "+getPipelineConfigFilePath().getAbsolutePath());

		} catch (IOException e) {
			LOGGER.log(Level.WARNING, "Failed to save "+getPipelineConfigFilePath().getAbsolutePath(),e);
		}

		// clone/pull configuration repository
		File configRepoFolder = new File(Jenkins.getInstance().getRootDir(), "pipeline/jenkins_config");
		String configRepoURL = Jenkins.getInstance().getDescriptorByType(CobPipelineProperty.DescriptorImpl.class).getConfigRepoURL();
		Git git = new Git(new FileRepository(configRepoFolder + "/.git"));

		// check if config repo exists
		if (!configRepoFolder.isDirectory()) {
			try {
				Git.cloneRepository()
					.setURI(configRepoURL)
					.setDirectory(configRepoFolder)
					.call();
				LOGGER.log(Level.INFO, "Successfully cloned configuration repository from "+configRepoURL);
			} catch (Exception ex) {
				LOGGER.log(Level.WARNING, "Failed to clone configuration repository", ex);
			}
		} else {
			try {
				git.pull().call();
				LOGGER.log(Level.INFO, "Successfully pulled configuration repository from "+configRepoURL);
			} catch (Exception ex) {
				LOGGER.log(Level.WARNING, "Failed to pull configuration repository", ex);
			}
		}

		// copy pipeline-config.yaml into repository
		File configRepoFile = new File(configRepoFolder, this.masterName+"/"+this.userName+"/");
		if (!configRepoFile.isDirectory()) configRepoFile.mkdirs();
		String[] cpCommand = {"cp", "-f", getPipelineConfigFilePath().getAbsolutePath(), configRepoFile.getAbsolutePath()}; // TODO check if repo exists; cp to subfolder 'server/user'

		Runtime rt = Runtime.getRuntime();
		Process proc;
		BufferedReader readIn, readErr = null;

		try {
			proc = rt.exec(cpCommand);
			readIn = new BufferedReader(new InputStreamReader(proc.getInputStream()));
			readErr = new BufferedReader(new InputStreamReader(proc.getErrorStream()));
			LOGGER.log(Level.INFO, "Successfully copied "+getPipelineConfigFilePath().getAbsolutePath()+" to config repository: "+configRepoFile.getAbsolutePath());
			if (readIn.readLine()!=null) LOGGER.log(Level.INFO, readIn.readLine());
		} catch (IOException e) {
			LOGGER.log(Level.WARNING, "Failed to copy "+getPipelineConfigFilePath().getAbsolutePath()+" to config repository: "+configRepoFile.getAbsolutePath(),e);
			if (readErr.readLine()!=null) LOGGER.log(Level.WARNING, readErr.readLine());
		}
		
		// add
		try {
			git.add().addFilepattern(this.masterName+"/"+this.userName+"/pipeline_config.yaml").call();
			LOGGER.log(Level.INFO, "Successfully added file to configuration repository");
		} catch (Exception e) {
			LOGGER.log(Level.WARNING, "Failed to add "+this.masterName+"/"+this.userName+"/pipeline_config.yaml",e);
		}

		// commit
		try {
			git.commit().setMessage("Updated pipeline for"+this.userName).call();
		} catch (Exception e) {
			LOGGER.log(Level.WARNING, "Failed to commit change in "+this.masterName+"/"+this.userName+"/pipeline_config.yaml",e);
		}

		// push
		try {
			git.push().call();
			LOGGER.log(Level.INFO, "Successfully pushed configuration repository");
		} catch (Exception e) {
			LOGGER.log(Level.WARNING, "Failed to push configuration repository",e);
		}

		// TODO trigger python generation script 
	}

	private Writer getPipelineConfigFile() throws IOException {
		return new FileWriter(getPipelineConfigFilePath());
	}

	private File getPipelineConfigFilePath() {
		return new File(Jenkins.getInstance().getRootDir(), "users/"+user.getId()+"/pipeline_config.yaml");
	}

	private static final Logger LOGGER = Logger.getLogger(Descriptor.class.getName());

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
