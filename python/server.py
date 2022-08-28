import bluetooth
import RPi.GPIO as GPIO

FORWARD = 11
BACKWARDS = 37
HALL = 36

#

GPIO.setmode(GPIO.BOARD)
GPIO.setwarnings(False)

GPIO.setup(FORWARD, GPIO.OUT)
GPIO.setup(BACKWARDS, GPIO.OUT)
GPIO.setup(HALL, GPIO.IN)

OPEN = 1
OPENING = 2

CLOSE = 6
CLOSING = 7

STOPPED = 9

door_state = CLOSE

MAX_ROTATIONS = 295
rotations = 0

#

def open():
    GPIO.output(BACKWARDS, 1)
    GPIO.output(FORWARD, 0)
    return


def close():
    GPIO.output(BACKWARDS, 0)
    GPIO.output(FORWARD, 1)
    return


def reset():
    GPIO.output(BACKWARDS, 0)
    GPIO.output(FORWARD, 0)
    return

#

def rotation_changed(channel):
    global rotations
    rotations = rotations + 1

    if (rotations >= MAX_ROTATIONS):
        rotations = 0
    reset()

    print("rotation %s" % rotations)
    # print('hall change detected on channel %s' % channel)
    return

GPIO.add_event_detect(HALL, GPIO.RISING, callback=rotation_changed, bouncetime=50)

def listen():
    try:
        global door_state
        global server_socket

        reset()
        door_state = CLOSE

        #

        server_socket = bluetooth.BluetoothSocket(bluetooth.RFCOMM)

        port = 1
        server_socket.bind(("", port))
        server_socket.listen(1)

        client_socket, address = server_socket.accept()
        print "Accepted connection from ", address
        while 1:
            
            data = client_socket.recv(1024)
            print "Received: %s" % data
            
            if (data == "stat"):
                client_socket.send("%s" % door_state)

            if (data == "open"):
                door_state = OPENING
                
                open()
                print("set_state %s" % door_state)
                client_socket.send("%s" % door_state)
    
            elif(data == "stop"):
                reset()
                client_socket.send("%s" % STOPPED)
            elif(data == "resume"):  # TODO: tato logika jde zjednodusit pokud bych odstranil string flagy
                if (door_state == OPENING):
                    close()
                    door_state = CLOSING
                elif(door_state == CLOSING):
                    open()
                    door_state = OPENING

                print("set_state %s" % door_state)
                client_socket.send("%s" % door_state)
            elif(data == "close"):
                door_state = CLOSING
                close()

            print("set_state %s" % door_state)
            client_socket.send("%s" % door_state)

            if (data == "q"):
                print("Quit")
                break

    except Exception as e:
        print(e)
    print "client disconnected"

    listen()

    server_socket.close()
    client_socket.close()
    return

#

listen()

server_socket.close()

reset()
