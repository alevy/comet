#!/usr/bin/env ruby

include 'arrays.rb'

hash = Hash.new
while(line = gets.split(","))
  hash[line[0]] ||= []
  hash[line[0]] << line[1].to_i
end

hash.each |k,v| do
  puts "#{k},#{v.mean},#{v.stdev}"
end

