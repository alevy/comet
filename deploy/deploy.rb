#!/usr/bin/env ruby

$: << File.dirname(__FILE__)

require 'comet_config'
require 'yaml'

config_file = ARGV.first

raise "You must specifcy deployment configuration file." unless config_file
raise "Deployment configuration file #{config_file} does not exist" unless File.exist?(config_file)

config = CometConfig.new(YAML::load(File.read(config_file)))

if config.copyjar
  config.nodes.each do |host|
    Process.fork do
      Kernel.exec("scp", "-o StrictHostKeyChecking=no",
        config.jarfile,
        "#{host}:#{config.rwd}")
    end
    Process.fork do
          Kernel.exec("scp", "-o StrictHostKeyChecking=no",
            File.join(File.dirname(__FILE__), "/background.sh"),
            "#{host}:#{config.rwd}")
        end
  end
end

if config.start_bootstrap
  Process.fork do
    Kernel.exec("ssh", "-o StrictHostKeyChecking=no",
      config.bootstrap, "bash",
      File.join(config.rwd, "background.sh"),
      config.rwd,
      "\"#{config.java} -classpath #{File.join(config.rwd, config.jarbasename)} edu.washington.cs.activedht.expt.ActivePeer -l -h #{config.bootstrap} -p #{config.bootstrap_port} -b #{config.bootstrap_address}\"")
  end
  sleep(5)
end

config.nodes.each do |host|
  config.ports.each do |port|
    Process.fork do
      Kernel.exec("ssh", "-o StrictHostKeyChecking=no",
        host, "bash",
        File.join(config.rwd, "background.sh"),
        config.rwd,
        "\"#{config.java} -classpath #{File.join(config.rwd, config.jarbasename)} edu.washington.cs.activedht.expt.ActivePeer -l -h #{host} -p #{port} -b #{config.bootstrap_address}\"")
    end
  end
end
#"screen -d -m ~/workspace/activedht/src/runclass.sh edu.washington.cs.activedht.expt.ActivePeer -h nethack.cs.washington.edu -p 4321 -b nethack.cs.washington.edu:4321")