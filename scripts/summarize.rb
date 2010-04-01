#!/usr/bin/env ruby

$:.unshift File.dirname(__FILE__)

require 'arrays'

scale = (ARGV.pop || 1).to_i

hash = Hash.new
while(line = gets)
  k,v = line.split(',')
  hash[k.to_i] ||= []
  hash[k.to_i] << v.to_i
end

hash.sort.each do |k,v|
  puts "#{k},#{v.mean / scale},#{v.stdev}"
end

