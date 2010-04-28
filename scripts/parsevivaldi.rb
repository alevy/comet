#!/usr/bin/env ruby

for file in ARGV

  name = File.basename(file)
  file = File.open(file, "r")

  result = Hash.new

  file.gets
  while line = file.gets
    match = line.match(/([0-9]+),(.*)$/)
    time = match[1].to_i
    match = match[2].split(/[,=]/).select {|e| e =~ /^[^\[\]]+$/}
    result[time] = match
  end

  result.sort.each do |k,v|
    v.each do |n|
      puts "#{name},#{n},#{k}"
    end
  end
end
