'use strict';

const counterEl = document.querySelector('#counter');
const xEl = document.querySelector('#x');
const sumEl = document.querySelector('#sum');


const connection = new autobahn.Connection({
  url: "ws://localhost:8080/ws",
  realm: "hello.realm"
});


let i1, i2;
connection.onopen = function (session) {

  // session.subscribe('hello.counter', (data) => counterEl.innerText = data[0]);

  /*i1 = setInterval(function() {
    session.publish('hello.greeting', ['Hello from JavaScript (browser)']);
  }, 1000);*/

  // session.register('hello.multiply', (x, y) => x * y);
  
  let x = 0;
  i2 = setInterval(function() {
    xEl.innerText = x;
    session.call('hello.sum', [x, 18]).then((s) => sumEl.innerText = '#'+s);
    x = x + 3;
  }, 1000);
};


connection.onclose = function () {
  if (t1) clearInterval(t1);
  if (t2) clearInterval(t2);
};


connection.open();