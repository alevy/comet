#!/usr/bin/env ruby

$: << File.dirname(__FILE__)

require 'comet_config'
require 'yaml'
require 'erb'

config_file = ARGV.shift
args = ARGV.join(" ")

raise "You must specifcy deployment configuration file." unless config_file
raise "Deployment configuration file #{config_file} does not exist" unless File.exist?(config_file)

@basedir = File.dirname(__FILE__)
config = CometConfig.new(YAML::load(ERB.new(File.read(config_file)).result(binding)))

if config.copyjar
  config.nodes.each do |host|
    puts "Copying to... #{host}"
    Process.fork do
      Kernel.exec("scp", "-o StrictHostKeyChecking=no",
        config.jarfile,
        "#{config.userat}#{host}:#{config.rwd}")
    end
    Process.fork do
      Kernel.exec("scp", "-o StrictHostKeyChecking=no",
        File.join(File.dirname(__FILE__), "/background.sh"),
        "#{config.userat}#{host}:#{config.rwd}")
    end
    Process.fork do
      Kernel.exec("scp", "-o StrictHostKeyChecking=no",
        File.join(File.dirname(__FILE__), "/kill.sh"),
        "#{config.userat}#{host}:#{config.rwd}")
    end
  end
  Process.waitall
end

if config.start_bootstrap
  Process.fork do
    Kernel.exec("ssh", "-o StrictHostKeyChecking=no",
      config.userat + config.bootstrap, "bash",
      File.join(config.rwd, "background.sh"),
      config.rwd,
      "\"#{config.java} -Xmx64m -classpath #{File.join(config.rwd, config.jarbasename)} edu.washington.cs.activedht.expt.ActivePeer -l ALL -h #{config.bootstrap} -p #{config.bootstrap_port} -b #{config.bootstrap_address} #{args}\"")
  end
  Process.waitall
  sleep(5)
end

config.nodes.each do |host|
  config.ports.each do |port|
    puts "Starting... #{host}:#{port}"
    Process.fork do
      Kernel.exec("ssh", "-o StrictHostKeyChecking=no",
        config.userat + host, "bash",
        File.join(config.rwd, "background.sh"),
        config.rwd,
        "\"#{config.java} -Xmx64m -classpath #{File.join(config.rwd, config.jarbasename)} edu.washington.cs.activedht.expt.ActivePeer -l WARNING -h #{host} -p #{port} -b #{config.bootstrap_address} #{args}\"")
    end
  end
end
Process.waitall
