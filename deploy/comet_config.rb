class CometConfig
  
  attr_reader :copyjar
  attr_reader :jarfile
  attr_reader :rwd
  attr_reader :java
  attr_reader :ports
  attr_reader :bootstrap
  attr_reader :bootstrap_port
  attr_reader :nodes
  attr_reader :start_bootstrap
  
  def initialize(hash)
    hash.each do |key,val|
      instance_variable_set("@#{key}", val)
    end
  end
  
  def hosts
    [@bootstrap] + @nodes
  end
  
  def jarbasename
    File.basename(@jarfile)
  end
  
  def bootstrap_address
    [@bootstrap,@bootstrap_port].join(":")
  end
  
end