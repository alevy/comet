#!/usr/bin/env ruby

$: << File.dirname(__FILE__)

require 'comet_config'
require 'yaml'

config_file = ARGV.first

raise "You must specifcy deployment configuration file." unless config_file
raise "Deployment configuration file #{config_file} does not exist" unless File.exist?(config_file)

config = CometConfig.new(YAML::load(File.read(config_file)))

if config.start_bootstrap
  Process.fork do
    Kernel.exec("ssh", "-o StrictHostKeyChecking=no",
      config.bootstrap, "bash",
      File.join(config.rwd, "kill.sh"),
      config.rwd)
  end
end

config.nodes.each do |host|
  config.ports.each do |port|
    Process.fork do
      Kernel.exec("ssh", "-o StrictHostKeyChecking=no",
        host, "bash",
        File.join(config.rwd, "kill.sh"),
        config.rwd)
    end
  end
end
