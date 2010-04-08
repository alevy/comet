#!/usr/bin/env ruby

$: << File.dirname(__FILE__)

require 'comet_config'
require 'yaml'
require 'erb'

config_file = ARGV.shift
output_dir = ARGV.shift

raise "You must specifcy deployment configuration file." unless config_file
raise "Deployment configuration file #{config_file} does not exist" unless File.exist?(config_file)

@basedir = File.dirname(__FILE__)
config = CometConfig.new(YAML::load(ERB.new(File.read(config_file)).result(binding)))

config.ports.each do |port|
  puts `#{File.dirname(__FILE__)}/../runclass.sh edu.washington.cs.activedht.expt.remote.Ping #{config.nodes.join(":" + port.to_s + " ") + ":" + port.to_s}`
end
