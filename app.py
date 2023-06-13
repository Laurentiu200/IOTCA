from flask import Flask
from flask import render_template, url_for
import pyrebase
from datetime import datetime

import pyrebase


config = {
  "apiKey": "AIzaSyA8y6gxPaNJ-iSjjA3X295lZw7WD4M4uvw",
  "authDomain": "iotca-2aaf0.firebaseapp.com",
  "databaseURL": "https://iotca-2aaf0-default-rtdb.firebaseio.com/",
  "storageBucket": "iotca-2aaf0.appspot.com"
}

firebase = pyrebase.initialize_app(config)

db = firebase.database()

app = Flask(__name__, static_url_path ='/static')

@app.route("/")
def basic():
    readings = db.child("Status").get().val()
    values = list(readings.items())
    humidity = values[0][1]
    temperature = values[1][1] 
    now = datetime.now()
    current_time = now.strftime("%H:%M:%S")
    return render_template('index.html', humid = humidity, temp = temperature, crt_time = current_time)


if __name__== "__main__":
    app.run()