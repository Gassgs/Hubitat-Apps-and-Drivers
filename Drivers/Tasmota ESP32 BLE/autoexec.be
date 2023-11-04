tasmota.add_rule('IBEACON#NAME=beacon1', 
  def (value,trigger,jsonmap)
    var status=jsonmap['IBEACON']['STATE']
    var relay=tasmota.get_power(0)
    if status == 'ON' && relay == false
      tasmota.set_power(0, true)
    end
  end)
tasmota.add_rule('IBEACON#NAME=beacon1', 
  def (value,trigger,jsonmap)
    var status=jsonmap['IBEACON']['STATE']
    var relay=tasmota.get_power(0)
    if status == 'OFF' && relay == true
      tasmota.set_power(0, false)
    end
  end)
tasmota.add_rule('IBEACON#NAME=beacon2', 
  def (value,trigger,jsonmap)
    var status=jsonmap['IBEACON']['STATE']
    var relay=tasmota.get_power(1)
    if status == 'ON' && relay == false
      tasmota.set_power(1, true)
    end
  end)
tasmota.add_rule('IBEACON#NAME=beacon2', 
  def (value,trigger,jsonmap)
    var status=jsonmap['IBEACON']['STATE']
    var relay=tasmota.get_power(1)
    if status == 'OFF' && relay == true
      tasmota.set_power(1, false)
    end
  end)
tasmota.add_rule('IBEACON#NAME=beacon3', 
  def (value,trigger,jsonmap)
    var status=jsonmap['IBEACON']['STATE']
    var relay=tasmota.get_power(2)
    if status == 'ON' && relay == false
      tasmota.set_power(2, true)
    end
  end)
tasmota.add_rule('IBEACON#NAME=beacon3', 
  def (value,trigger,jsonmap)
    var status=jsonmap['IBEACON']['STATE']
    var relay=tasmota.get_power(2)
    if status == 'OFF' && relay == true
      tasmota.set_power(2, false)
    end
  end)
  tasmota.add_rule('IBEACON#NAME=beacon4', 
  def (value,trigger,jsonmap)
    var status=jsonmap['IBEACON']['STATE']
    var relay=tasmota.get_power(3)
    if status == 'ON' && relay == false
      tasmota.set_power(3, true)
    end
  end)
tasmota.add_rule('IBEACON#NAME=beacon4', 
  def (value,trigger,jsonmap)
    var status=jsonmap['IBEACON']['STATE']
    var relay=tasmota.get_power(3)
    if status == 'OFF' && relay == true
      tasmota.set_power(3, false)
    end
  end)
tasmota.add_rule('IBEACON#NAME=beacon5', 
  def (value,trigger,jsonmap)
    var status=jsonmap['IBEACON']['STATE']
    var relay=tasmota.get_power(4)
    if status == 'ON' && relay == false
      tasmota.set_power(4, true)
    end
  end)
tasmota.add_rule('IBEACON#NAME=beacon5', 
  def (value,trigger,jsonmap)
    var status=jsonmap['IBEACON']['STATE']
    var relay=tasmota.get_power(4)
    if status == 'OFF' && relay == true
      tasmota.set_power(4, false)
    end
  end)
tasmota.add_rule('IBEACON#NAME=beacon6', 
  def (value,trigger,jsonmap)
    var status=jsonmap['IBEACON']['STATE']
    var relay=tasmota.get_power(5)
    if status == 'ON' && relay == false
      tasmota.set_power(5, true)
    end
  end)
tasmota.add_rule('IBEACON#NAME=beacon6', 
  def (value,trigger,jsonmap)
    var status=jsonmap['IBEACON']['STATE']
    var relay=tasmota.get_power(5)
    if status == 'OFF' && relay == true
      tasmota.set_power(5, false)
    end
  end)
  tasmota.add_rule('IBEACON#NAME=beacon7', 
  def (value,trigger,jsonmap)
    var status=jsonmap['IBEACON']['STATE']
    var relay=tasmota.get_power(6)
    if status == 'ON' && relay == false
      tasmota.set_power(6, true)
    end
  end)
tasmota.add_rule('IBEACON#NAME=beacon7', 
  def (value,trigger,jsonmap)
    var status=jsonmap['IBEACON']['STATE']
    var relay=tasmota.get_power(6)
    if status == 'OFF' && relay == true
      tasmota.set_power(6, false)
    end
  end)
  tasmota.add_rule('IBEACON#NAME=beacon8', 
  def (value,trigger,jsonmap)
    var status=jsonmap['IBEACON']['STATE']
    var relay=tasmota.get_power(7)
    if status == 'ON' && relay == false
      tasmota.set_power(7, true)
    end
  end)
tasmota.add_rule('IBEACON#NAME=beacon8', 
  def (value,trigger,jsonmap)
    var status=jsonmap['IBEACON']['STATE']
    var relay=tasmota.get_power(7)
    if status == 'OFF' && relay == true
      tasmota.set_power(7, false)
    end
  end)