#!/usr/bin/env ruby

$: << File.dirname(__FILE__)

require 'comet_config'
require 'yaml'
require 'erb'

config_file = ARGV.shift
tgz_file = ARGV.shift

raise "You must specifcy deployment configuration file." unless config_file
raise "Deployment configuration file #{config_file} does not exist" unless File.exist?(config_file)

@basedir = File.dirname(__FILE__)
config = CometConfig.new(YAML::load(ERB.new(File.read(config_file)).result(binding)))

config.nodes.each do |host|
  puts host
  Process.fork do
    Kernel.exec("scp", "-o StrictHostKeyChecking=no",
      tgz_file,
      "#{config.userat}#{host}:#{config.rwd}")
  end
end
Process.waitall

config.nodes.each do |host|
  puts host
  Process.fork do
    Kernel.exec("ssh", "-o StrictHostKeyChecking=no",
      config.userat + host, "tar",
      "xzf",
      File.basename(tgz_file))
  end
  Process.waitall
end
