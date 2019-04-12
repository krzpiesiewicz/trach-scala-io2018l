package game.core

import game.core.Card.CardId
import game.core.Player.PlayerId
import game.core.Attribute.AttributeTransformer

trait GameState {
  def attributes: AttributesSet[GlobalAttribute]
  
  lazy val playersMap = attributes.forceGet[Players].circleOfPlayers.playersMap
  
  lazy val cardsMap = attributes.forceGet[AllCards].cardsMap
  
  /**
   * Throws an exception if there is no card of id = cardId.
   */
  def card(cardId: CardId): Card = cardsMap.get(cardId).get
  
  /**
   * Throws an exception if there is no player of id = playerId.
   */
  def player(playerObj: Player): Player = player(playerObj.id)
  
  /**
   * Throws an exception if there is no player of id = playerId.
   */
  def player(playerId: PlayerId): Player = playersMap.get(playerId).get
  
  /**
   * Returns a new gamestate with attributes transformed by given transformer.
   */
  def transformed(transformer: AttributeTransformer[GlobalAttribute]): GameState
}

case class NormalState(override val attributes: AttributesSet[GlobalAttribute]) extends GameState {
  def transformed(transformer: AttributeTransformer[GlobalAttribute]) = new NormalState(attributes.transformed(transformer))
}

trait EndState extends GameState

object GameState {
  
  def removeCardFromPlayersHand(player: Player, card: Card, state: GameState): GameState = {
    if (player.hand.cards contains card) {
      val coveredStack = state.attributes.forceGet[CoveredCardsStack]
      val (poppedCardOpt, restOfStack) = coveredStack.pop match {
        case Some((poppedCard, restOfStack)) => (Some(poppedCard), restOfStack)
        case None => (None, coveredStack)
      }
      state transformed {
        case stack: CoveredCardsStack => restOfStack;
        case players: Players => players.updatePlayer(player.transformed {
          case hand: Hand => hand.replacedCard(card, poppedCardOpt);
        });
      }
    } else
      state
  }
}