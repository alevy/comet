#!/usr/bin/env ruby

port = 1234.to_s
hostname = "localhost"
bport = 4321.to_s
bhost = "localhost"

valueType = "KAHLUA"

for n in ((2..8).map {|i| 10**i})

  mpid = Process.fork do
      Kernel.exec("java", "-Xmx1000m","-classpath", File.join(File.dirname(__FILE__), "../dist/comet.jar"),
        "edu.washington.cs.activedht.expt.ActivePeer",
        "-p", bport,
        "-h", bhost,
        "-b", "#{bhost}:#{bport}",
        "-v", valueType)
  end
  
  sleep(60)
  
  cpid = Process.fork do
      Kernel.exec("java", "-Xmx1000m", "-classpath", File.join(File.dirname(__FILE__), "../dist/comet.jar"),
        "PutMany",
        "-p", port,
        "-h", hostname,
        "-b", "#{bhost}:#{bport}",
        "-n", n.to_s)
  end
  
  Process.wait(cpid)
  
  sleep(60)
  
  5.times do
    puts "#{n},#{`top -b -n 1 -p #{mpid} | grep #{mpid}`.strip.split(/\s+/)[5]}"
    sleep(10)
  end
  
  Process.kill("TERM", mpid)
  Process.wait(mpid)
end