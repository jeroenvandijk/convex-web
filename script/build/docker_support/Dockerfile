# Docker for convex web

FROM openjdk:11

ENV HOME=/home/convex-web

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

EXPOSE 8080/tcp
EXPOSE 5555/tcp

CMD java -Duser.home=$HOME -cp main.jar:deps.jar clojure.main -m convex-web.core