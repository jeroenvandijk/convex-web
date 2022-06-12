# Docker for convex web

FROM node:latest

ENV HOME=/home/convex-web

RUN apt-get update \
  && apt-get -q -y install openjdk-11-jdk curl

WORKDIR $HOME

ADD . $HOME

RUN rm -rf $HOME/.convex
RUN mkdir -p $HOME/.convex
RUN echo "{:key-store-passphrase \"password\" \
           :key-passphrase \"password\"}" > $HOME/.convex/secrets.edn

ADD config.edn $HOME/.convex/config.edn

RUN rm $HOME/config.edn

RUN ln -s $HOME/.convex/config.edn $HOME/config.edn

ADD deps.jar deps.jar

ADD main.jar main.jar

CMD java -Duser.home=$HOME cp:main.jar:deps.jar clojure.main -m convex-web.core