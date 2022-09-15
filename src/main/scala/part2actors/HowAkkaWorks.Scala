//     How Akka Works
/**
   * Akka has a thread pool that it shares with Actors. (ActiveThread = it can run code)
   *
   * Actor has Message Handler(receive) and Message Queue(MailBox)
   * Actor is a data-structure and it actually needs a thread to run.
   *
   * Akka spawns number of threads(100s) and these threads can handle huge amount of Actors(1000000s per GB heap).
   *
   * Akka manages to do that by scheduling actors for execution on these small number of threads.
   *
   * Mechanism:-
   *  Communication ----->
   *  Actor has a receive handler and some message enqueued in mailbox.

   *  1. Sending a Message:-
        a) message is enqueued in the actor's mailbox
        b) Thread Safe. Done by akka behind the scenes.

 *    2. Processing a message:- Actor is a ds and needs a thread to run the code...
        a) a thread is scheduled to run this actor.
            At some point, a thread will take control of this actor

        b) messages are executed from the mail box in order.
            Thread will start decoding messages from this actors mailbox.

        c) The thread invokes the handler on each message.
            For every one of those messages, thread will invoke the message handler. and as a result.
            An actor might change it's state or send message to another actors.
            After this message is simply discarded, this process happens bunch of times.

        d) At some point, Actor is unscheduled.
            At some point, AKKA thread scheduler decides to unscheduled this actor for execution, at
            which point thread releases control of this Actor and it will move on to do something else

      3. This whole process offers us some GUARANTEES
        a) Only 1 thread operates on an Actor at any time. Makes actors single threaded.

        b) No thread locks needed(No SYNCHRONIZATION needed)
            Only one thread has access to actors internal state at any time.

        c) The Thread may never release the actor in the middle of processing messages.
            Processing Messages is (ATOMIC)

      4. Message delivery GUARANTEES
        a) At most once delivery
            Receiving Actor receives at most once the message that i want to send it.
            It will never receive duplicates.

        b) For any sender-receiver pair, The Message ORDER is maintained
            1b. If i have 2 Actors, Alex sends Bob two messages, A-message followed by B-message.
                Bob will never receive a duplicate of either messages A or B

            2b. Bob will always receive message A before B.(always guaranteed)
                  Only if Bob receives messages from other actors, those might be intermingled.


 */
