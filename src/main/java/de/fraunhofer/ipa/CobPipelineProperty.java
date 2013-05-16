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
import hudson.Util;
import hudson.model.Descriptor;
import hudson.model.Descriptor.FormException;
import hudson.model.RootAction;
import hudson.model.User;
import hudson.model.UserProperty;
import hudson.model.UserPropertyDescriptor;
//import hudson.tasks.Mailer;
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
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletException;

import jenkins.model.Jenkins;
import net.sf.json.JSONObject;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.bind.JavaScriptMethod;

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
	
	private boolean committerEmailEnabled;

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
	public CobPipelineProperty() {
		this.rootRepos = rootRepos;
	}
	
	public void setEmail(String email) {
		this.email = email;
	}
	
	public String getEmail() {
		if(!this.email.isEmpty()) {
			return this.email;
		}
        /*if(this.user != null) {
            Mailer.UserProperty mailProperty = this.user.getProperty(Mailer.UserProperty.class);
            if (mailProperty != null) {
                return mailProperty.getAddress();
            }
        }*/
        return "";
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
	
	public void setCommitterEmailEnabled(boolean enabled) {
		this.committerEmailEnabled = enabled;
	}
	
	public boolean getCommitterEmailEnabled() {
		return this.committerEmailEnabled;
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

		private String jenkinsLogin;

		private String jenkinsPassword;
		
		private String pipelineDir;
		
		private String tarballLocation;

		private String githubLogin;

		private String githubPassword;

		private String pipelineReposOwner;

		private ArrayList<String> allRosDistros;

		private ArrayList<String> robots;

		private String targetsURL;

		private List<Map<String, List<String>>> targets;

		private String defaultFork;

		private String defaultBranch;

		public DescriptorImpl() {
			load();
		}

		@Override
		public String getDisplayName() {
			return "Pipeline Configurations";
		}

		@Override
		public UserProperty newInstance(User user) {
			return new CobPipelineProperty();
		}

		/**
		 * Checks if the given String is a email address
		 */
		public FormValidation doCheckEmail(@QueryParameter String value)
				throws IOException, ServletException {
			if (value.length()==0) {
				return FormValidation.error(Messages.Email_Empty());
			}
			return FormValidation.ok();
		}

		public void setJenkinsLogin(String jenkinsLogin) {
			this.jenkinsLogin = jenkinsLogin;
		}

		public String getJenkinsLogin() {
			return jenkinsLogin;
		}

		//TODO save password encrypted
		public void setJenkinsPassword(String jenkinsPassword) {
			this.jenkinsPassword = jenkinsPassword;
		}

		public String getJenkinsPassword() {
			return jenkinsPassword;
		}
		
		public void setPipelineDir(String pipelineDir) {
			this.pipelineDir = pipelineDir;
		}
		
		public String getPipelineDir() {
			return this.pipelineDir;
		}
		
		public void setTarballLocation(String tarballLocation) {
			this.tarballLocation = tarballLocation;
		}
		
		public String getTarballLocation() {
			return this.tarballLocation;
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

		public void setPipelineReposOwner(String pipelineReposOwner) {
			this.pipelineReposOwner = pipelineReposOwner;
		}

		public String getPipelineReposOwner() {
			return this.pipelineReposOwner;
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

		/**
		 * Checks if given String is valid Jenkins user
		 */
		public FormValidation doCheckJenkinsLogin(@QueryParameter String value) {
			//TODO
			return FormValidation.ok();
		}

		/**
		 * Checks if given password String is fits to Jenkins user
		 */
		public FormValidation doCheckJenkinsPassword(@QueryParameter String value, @QueryParameter String jenkinsLogin) {
			//TODO
			return FormValidation.ok();			
		}
		
		/**
		 * Checks if folder and jenkins_setup repository exist
		 */
		public FormValidation doCheckPipelineDir(@QueryParameter String value) {
			File pipeDir = new File(value);
			
			if (!pipeDir.exists()) {
				return FormValidation.error(Messages.PipelineDir_NotExistent());
			}
			if (!pipeDir.isDirectory()) {
				return FormValidation.error(Messages.PipelineDir_NotADirectory());
			}
			
			for (String inPipeDir : pipeDir.list()) {
				if (inPipeDir.equals("jenkins_setup")) {
					File setupDir = new File(value, inPipeDir);
					if (!setupDir.isDirectory()) {
						return FormValidation.error(Messages.PipelineDir_NoSetupDir(inPipeDir));
					}
					if (setupDir.list().length == 0) {
						return FormValidation.error(Messages.PipelineDir_RepoEmpty(inPipeDir));
					}
					File gitRepo = new File(setupDir, "/.git");
					if (!gitRepo.exists()) {
						return FormValidation.error(Messages.PipelineDir_NoGitRepo(inPipeDir));
					}
					return FormValidation.ok(Messages.PipelineDir_Ok());
				}
			}
			
			return FormValidation.error(Messages.PipelineDir_NoSetupRepo());
		}
		
		/**
		 * Checks if given URL exists
		 */
		public FormValidation doCheckTarballLocation(@QueryParameter String value) {
			//TODO
			return FormValidation.ok();			
		}

		/**
		 * Checks if given String is valid GitHub user
		 */
		public FormValidation doCheckGithubLogin(@QueryParameter String value)
				throws IOException, ServletException {
			if (value.length() == 0) {
				return FormValidation.error(Messages.Github_Login());
			} 
			try {
				UserService githubUserSrv = new UserService();
				githubUserSrv.getUser(value);
				return FormValidation.ok();
			} catch (IOException ex) {
				return FormValidation.error(Messages.Github_LoginInvalid() + "\n" + ex.getMessage());
			}
		}

		/**
		 * Checks if given password String is fits to GitHub user
		 */
		public FormValidation doCheckGithubPassword(@QueryParameter String value, @QueryParameter String githubLogin)
				throws IOException, ServletException {
			if (value.length() == 0) {
				return FormValidation.error(Messages.Github_Password());
			}
			try {
				GitHubClient client = new GitHubClient();
				client.setCredentials(githubLogin, value);
				UserService githubUserSrv = new UserService(client);
				org.eclipse.egit.github.core.User user = githubUserSrv.getUser(githubLogin);
				return FormValidation.ok("GitHub user name: "+user.getName()+"\nUser ownes "+
						user.getPublicRepos()+" public and "+user.getTotalPrivateRepos()+" private repositories");
			} catch (Exception ex) {
				return FormValidation.error(Messages.Github_PasswordIncorrect() + "\n" + ex.getMessage());
			}
		}
		
		public FormValidation doCheckPipelineReposOwner(@QueryParameter String value)
				throws IOException, ServletException {
			return doCheckGithubLogin(value);
		}

		public FormValidation doCheckTargets(@QueryParameter String value)
				throws IOException, ServletException {
			if (value.length() == 0) {
				return FormValidation.warning(Messages.Targets_Empty());
			}
			return FormValidation.ok();
		}

		public FormValidation doCheckDefaultFork(@QueryParameter String value)
				throws IOException, ServletException {
			if (value.length() == 0) {
				return FormValidation.error(Messages.DefaultFork_Empty());
			}
			
			this.defaultFork = value;

			return doCheckGithubLogin(value);
		}

		public String getDefaultFork() {
			return this.defaultFork;
		}

		public FormValidation doCheckDefaultBranch(@QueryParameter String value)
				throws IOException, ServletException {
			if (value.length() == 0) {
				return FormValidation.error(Messages.DefaultBranch_Empty());
			}
			this.defaultBranch = value;

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
		LOGGER.log(Level.INFO, "Saved user configuration"); //TODO
	}
		
	@JavaScriptMethod
	public JSONObject doGeneratePipeline() throws Exception {
		JSONObject response  = new JSONObject();
		String message = "";
		
		// wait until config.xml is updated
		File configFile = new File(Jenkins.getInstance().getRootDir() + "users", user.getId() + "/config.xml");
		Date modDate = new Date();
		Date start = modDate;
		Date now;
		do {
			try { 
			    Thread.sleep(1000);
			} catch(InterruptedException ex) {
			    Thread.currentThread().interrupt();
			}
			now = new Date();
			try {
				modDate = new Date(configFile.lastModified());
			} catch(Exception ex) {
				//if file was not created yet
				continue;
			}
			if (now.getTime() - start.getTime() > 30000) {
				throw new Exception("Timeout");
			}
		} while (now.getTime() - modDate.getTime() > 15000 || now.getTime() - modDate.getTime() <= 0);
				
		try {
			Map<String, Object> data = new HashMap<String, Object>();
			data.put("user_name", user.getId());
			data.put("server_name", getMasterName());
			data.put("email", this.email);
			data.put("committer_email_enabled", this.committerEmailEnabled);
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
					deps.put(repoDep.name, dep);
				}
				repo.put("dependencies", deps);

				repos.put(rootRepo.fullName, repo);
			}
			data.put("repositories", repos);
			Yaml yaml = new Yaml();
			yaml.dump(data, getPipelineConfigFile());
			LOGGER.log(Level.INFO, "Created "+getPipelineConfigFilePath().getAbsolutePath()); //TODO

		} catch (IOException e) {
			LOGGER.log(Level.WARNING, "Failed to save "+getPipelineConfigFilePath().getAbsolutePath(),e); //TODO
		}

		// clone/pull configuration repository
		File configRepoFolder = new File(Jenkins.getInstance().getDescriptorByType(CobPipelineProperty.DescriptorImpl.class).getPipelineDir(), "jenkins_config");
		String configRepoURL = "git@github.com:" + Jenkins.getInstance().getDescriptorByType(CobPipelineProperty.DescriptorImpl.class).getPipelineReposOwner() + "/jenkins_config.git";
		Git git = new Git(new FileRepository(configRepoFolder + "/.git"));

		// check if configuration repository exists
		if (!configRepoFolder.isDirectory()) {
			try {
				Git.cloneRepository()
					.setURI(configRepoURL)
					.setDirectory(configRepoFolder)
					.call();
				LOGGER.log(Level.INFO, "Successfully cloned configuration repository from "+configRepoURL); //TODO
			} catch (Exception ex) {
				LOGGER.log(Level.WARNING, "Failed to clone configuration repository", ex); //TODO
			}
		} else {
			try {
				git.pull().call();
				LOGGER.log(Level.INFO, "Successfully pulled configuration repository from "+configRepoURL); //TODO
			} catch (Exception ex) {
				LOGGER.log(Level.WARNING, "Failed to pull configuration repository", ex); //TODO
			}
		}
		
		// copy pipeline-config.yaml into repository
		File configRepoFile = new File(configRepoFolder, getMasterName()+"/"+user.getId()+"/");
		if (!configRepoFile.isDirectory()) configRepoFile.mkdirs();
		String[] cpCommand = {"cp", "-f", getPipelineConfigFilePath().getAbsolutePath(), configRepoFile.getAbsolutePath()};

		Runtime rt = Runtime.getRuntime();
		Process proc;
		BufferedReader readIn, readErr;
		String s, feedback;
		proc = rt.exec(cpCommand);
		readIn = new BufferedReader(new InputStreamReader(proc.getInputStream()));
		readErr = new BufferedReader(new InputStreamReader(proc.getErrorStream()));
		feedback = "";
		while ((s = readErr.readLine()) != null) feedback += s+"\n";
		if (feedback.length()!=0) {
			LOGGER.log(Level.WARNING, "Failed to copy "+getPipelineConfigFilePath().getAbsolutePath()+" to config repository: "+configRepoFile.getAbsolutePath()); //TODO
			LOGGER.log(Level.WARNING, feedback); //TODO
		}
		else {
			LOGGER.log(Level.INFO, "Successfully copied "+getPipelineConfigFilePath().getAbsolutePath()+" to config repository: "+configRepoFile.getAbsolutePath()); //TODO
		}
		
		// add
		try {
			git.add().addFilepattern(getMasterName()+"/"+user.getId()+"/pipeline_config.yaml").call();
			LOGGER.log(Level.INFO, "Successfully added file to configuration repository"); //TODO
		} catch (Exception e) {
			LOGGER.log(Level.WARNING, "Failed to add "+getMasterName()+"/"+user.getId()+"/pipeline_config.yaml",e); //TODO
		}

		// commit
		try {
			git.commit().setMessage("Updated pipeline configuration for "+user.getId()).call();
		} catch (Exception e) {
			LOGGER.log(Level.WARNING, "Failed to commit change in "+getMasterName()+"/"+user.getId()+"/pipeline_config.yaml",e); //TODO
		}

		// push
		try {
			git.push().call();
			LOGGER.log(Level.INFO, "Successfully pushed configuration repository"); //TODO
		} catch (Exception e) {
			LOGGER.log(Level.WARNING, "Failed to push configuration repository",e); //TODO
		}

		// trigger Python job generation script
		String[] generationCall = {new File(Jenkins.getInstance().getDescriptorByType(CobPipelineProperty.DescriptorImpl.class).getPipelineDir(), "jenkins_setup/scripts/generate_buildpipeline.py").toString(),
				"-m", Jenkins.getInstance().getRootUrl(),
				"-l", Jenkins.getInstance().getDescriptorByType(CobPipelineProperty.DescriptorImpl.class).getJenkinsLogin(),
				"-p", Jenkins.getInstance().getDescriptorByType(CobPipelineProperty.DescriptorImpl.class).getJenkinsPassword(),
				"-o", Jenkins.getInstance().getDescriptorByType(CobPipelineProperty.DescriptorImpl.class).getPipelineReposOwner(),
				"-t", Jenkins.getInstance().getDescriptorByType(CobPipelineProperty.DescriptorImpl.class).getTarballLocation(),
				"-u", user.getId()};
		
		proc = rt.exec(generationCall);
		readIn = new BufferedReader(new InputStreamReader(proc.getInputStream()));
		readErr = new BufferedReader(new InputStreamReader(proc.getErrorStream()));
		feedback = "";
		while ((s = readErr.readLine()) != null) feedback += s+"\n";
		if (feedback.length()!=0) {
			LOGGER.log(Level.WARNING, "Failed to generate pipeline: "); //TODO
			LOGGER.log(Level.WARNING, feedback);
			response.put("message", feedback.replace("\n", "<br/>"));
			response.put("status", "<font color=\"red\">" + Messages.Pipeline_GenerationFailure() + "</font>");
			return response;
		} else {
			feedback = "";
			while ((s = readIn.readLine()) != null) feedback += s+"\n";
			if (feedback.length()!=0) {
				LOGGER.log(Level.INFO, feedback);
				LOGGER.log(Level.INFO, "Successfully generated pipeline"); //TODO
				message += feedback;
			}
		}
		response.put("message", message.replace("\n", "<br/>"));
		response.put("status", "<font color=\"green\">" + Messages.Pipeline_GenerationSuccess() + "</font>");
		return response;
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
			return Messages.Pipeline_DisplayName();
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
