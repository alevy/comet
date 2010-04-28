#!/usr/bin/env ruby

$: << File.dirname(__FILE__)

require 'comet_config'
require 'yaml'
require 'erb'

config_file = ARGV.shift
file = ARGV.shift
output_dir = ARGV.shift

raise "You must specifcy deployment configuration file." unless config_file
raise "Deployment configuration file #{config_file} does not exist" unless File.exist?(config_file)

@basedir = File.dirname(__FILE__)
config = CometConfig.new(YAML::load(ERB.new(File.read(config_file)).result(binding)))

config.nodes.each do |host|
  Process.fork do
    File.open(File.join(output_dir, host), 'w') {|f| f.write(`ssh -o StrictHostKeyChecking=no #{config.userat + host} "cat #{file}"`) }
  end
end
Process.waitall
