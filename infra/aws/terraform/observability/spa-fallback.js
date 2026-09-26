function handler(event) {
  var request = event.request;
  var uri = request.uri;
  if (uri.indexOf('/v1/') === 0 || uri.indexOf('/actuator/') === 0 || uri.indexOf('/assets/') === 0) {
    return request;
  }
  if (uri.indexOf('.') === -1) {
    request.uri = '/index.html';
  }
  return request;
}
